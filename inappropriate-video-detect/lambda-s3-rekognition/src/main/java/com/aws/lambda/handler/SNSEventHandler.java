package com.aws.lambda.handler;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ContentModerationDetection;
import software.amazon.awssdk.services.rekognition.model.GetContentModerationRequest;
import software.amazon.awssdk.services.rekognition.model.GetContentModerationResponse;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.VideoMetadata;

public class SNSEventHandler implements RequestHandler<SNSEvent, String> {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public String handleRequest(SNSEvent event, Context context) {
		LambdaLogger logger = context.getLogger();

		logger.log("event:" + event);
		logger.log("event json: " + gson.toJson(event));

		List<SNSRecord> records = event.getRecords();
		SNSRecord record = records.get(0);
		SNS sns = record.getSNS();
		String message = sns.getMessage();
		logger.log("message result:" + message);
		
		JsonObject result = gson.fromJson(message, JsonObject.class);
		logger.log("JobId=" + result.get("JobId").getAsString());
		logger.log("Status=" + result.get("Status").getAsString());
		
		logger.log("start to fetch-------------------------");
		RekognitionClient rekClient = RekognitionClient.builder().build();
		String startJobId = result.get("JobId").getAsString();
		try {
            String paginationToken=null;
            GetContentModerationResponse modDetectionResponse=null;
            boolean finished = false;
            String status;
            int yy=0 ;

            do{
                if (modDetectionResponse !=null) {
                    paginationToken = modDetectionResponse.nextToken();
                }
                logger.log("tring to fetch -----------");
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
                logger.log("Format: " + videoMetaData.format());
                logger.log("Codec: " + videoMetaData.codec());
                logger.log("Duration: " + videoMetaData.durationMillis());
                logger.log("FrameRate: " + videoMetaData.frameRate());
                logger.log("Job");

                List<ContentModerationDetection> mods = modDetectionResponse.moderationLabels();
                for (ContentModerationDetection mod: mods) {
                    long seconds=mod.timestamp()/1000;
                    logger.log("Mod label: " + seconds + " ");
                    logger.log(mod.moderationLabel().toString());
                    logger.log("-------------------------");
                }

            } while (modDetectionResponse !=null && modDetectionResponse.nextToken() != null);
            
            rekClient.close();

        } catch(RekognitionException | InterruptedException e) {
        	logger.log(e.getMessage());
        }

		return "OK";
	}

}
