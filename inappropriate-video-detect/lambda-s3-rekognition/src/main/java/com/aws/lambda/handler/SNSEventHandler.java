package com.aws.lambda.handler;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.aws.lambda.dao.VideoDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ContentModerationDetection;
import software.amazon.awssdk.services.rekognition.model.GetContentModerationRequest;
import software.amazon.awssdk.services.rekognition.model.GetContentModerationResponse;
import software.amazon.awssdk.services.rekognition.model.VideoMetadata;

/**
 * SNS notify trigger lambda to fetch the results from rekognition
 * @author Carl
 *
 */
public class SNSEventHandler implements RequestHandler<SNSEvent, String> {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public String handleRequest(SNSEvent event, Context context) {
		LambdaLogger logger = context.getLogger();
		
		logger.log("step1 - get the SNS event request ------------");
		logger.log("event json: " + gson.toJson(event));

		List<SNSRecord> records = event.getRecords();
		SNSRecord record = records.get(0);
		SNS sns = record.getSNS();
		String message = sns.getMessage();
		logger.log("Message result:" + message);
		
		JsonObject result = gson.fromJson(message, JsonObject.class);
		logger.log("parse get JobId: " + result.get("JobId").getAsString());
		logger.log("parse get Status: " + result.get("Status").getAsString());
		JsonObject videoObj = result.get("Video").getAsJsonObject();
		String fileName = videoObj.get("S3ObjectName").getAsString();
		String id = fileName.substring(0, fileName.length() - 4);
		logger.log("parse get hash ID: " + id);
		
		RekognitionClient rekClient = RekognitionClient.builder().build();
		String startJobId = result.get("JobId").getAsString();
		try {
            String paginationToken=null;
            GetContentModerationResponse modDetectionResponse=null;
            boolean finished = false;
            String status;
            int yy=0 ;

            StringBuilder sb = new StringBuilder();
            do{
                if (modDetectionResponse !=null) {
                    paginationToken = modDetectionResponse.nextToken();
                }
                GetContentModerationRequest modRequest = GetContentModerationRequest.builder()
                    .jobId(startJobId)
                    .nextToken(paginationToken)
                    .maxResults(10)
                    .build();

                // Wait until the job succeeds
                while (!finished) {
                    modDetectionResponse = rekClient.getContentModeration(modRequest);
                    status = modDetectionResponse.jobStatusAsString();

                    if (status.compareTo("SUCCEEDED") == 0)
                        finished = true;
                    else {
                        logger.log(yy + " status is: " + status);
                        Thread.sleep(1000);
                    }
                    yy++;
                }

                finished = false;

                // Proceed when the job is done - otherwise VideoMetadata is null
                VideoMetadata videoMetaData=modDetectionResponse.videoMetadata();
                logger.log("step2 - get the video results from rekognition--------");
                logger.log("Format: " + videoMetaData.format());
                logger.log("Codec: " + videoMetaData.codec());
                logger.log("Duration: " + videoMetaData.durationMillis());
                logger.log("FrameRate: " + videoMetaData.frameRate());

                List<ContentModerationDetection> mods = modDetectionResponse.moderationLabels();
                for (ContentModerationDetection mod: mods) {
                    long seconds=mod.timestamp()/1000;
                    logger.log("Mod label: " + seconds + " ");
                    logger.log(mod.moderationLabel().toString());
                    sb.append(mod.moderationLabel().toString());
                    logger.log("-------------------------");
                }

            } while (modDetectionResponse !=null && modDetectionResponse.nextToken() != null);
            
            VideoDO item = new VideoDO();
            item.setId(id);
            item.setJobID(startJobId);
            item.setDetail(sb.toString());
            if (sb.toString().isEmpty()) {
            	item.setResult("KIDS");
            } else {
            	item.setResult("ALDULTS");
            }
            item.save(item);
            logger.log("step3 - save the results to DynamoDB-------------");
            logger.log("step4 - TODO invoke call back URL, notifications email, slack");
            rekClient.close();
        } catch(Exception e) {
        	logger.log(e.getMessage());
        }
		return "OK";
	}

}
