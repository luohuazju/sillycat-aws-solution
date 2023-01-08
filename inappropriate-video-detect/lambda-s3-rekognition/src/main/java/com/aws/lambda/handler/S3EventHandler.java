package com.aws.lambda.handler;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.ContentModerationDetection;
import software.amazon.awssdk.services.rekognition.model.GetContentModerationRequest;
import software.amazon.awssdk.services.rekognition.model.GetContentModerationResponse;
import software.amazon.awssdk.services.rekognition.model.NotificationChannel;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.StartContentModerationRequest;
import software.amazon.awssdk.services.rekognition.model.StartContentModerationResponse;
import software.amazon.awssdk.services.rekognition.model.Video;
import software.amazon.awssdk.services.rekognition.model.VideoMetadata;

public class S3EventHandler implements RequestHandler<S3Event, String> {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public String handleRequest(S3Event event, Context context) {
		LambdaLogger logger = context.getLogger();

		logger.log("event:" + event);
		logger.log("event json: " + gson.toJson(event));
		try {
			// Get Event Record
			S3EventNotificationRecord record = event.getRecords().get(0);
			// Source File Name
			String srcFileName = record.getS3().getObject().getKey();
			logger.log("fileName:" + srcFileName);
			String snsArn = System.getenv("rekognitionSNSArn");
			logger.log("SNSARN:" + snsArn);
			String roleArn = System.getenv("roleArn");
			logger.log("roleArn:" + roleArn);

			RekognitionClient rekClient = RekognitionClient.builder().build();
			logger.log("1 create rek client done");
			NotificationChannel channel = NotificationChannel.builder().snsTopicArn(snsArn).roleArn(roleArn).build();
			logger.log("2 create sns client done");

			S3Object s3Obj = S3Object.builder().bucket("rekognition-videos-bucket").name(srcFileName).build();
			logger.log("3 s3 resource done");
			Video vidOb = Video.builder().s3Object(s3Obj).build();
			logger.log("4 video ready to check");

			StartContentModerationRequest modDetectionRequest = StartContentModerationRequest.builder()
					.jobTag("Moderation").notificationChannel(channel).video(vidOb).build();
			logger.log("5 build request");

			StartContentModerationResponse startModDetectionResult = rekClient
					.startContentModeration(modDetectionRequest);
			logger.log("6 send out request");
			String startJobId = startModDetectionResult.jobId();
			logger.log("7 get taskID");
			logger.log("start to check the video in jobID=" + startJobId);
			
			logger.log("start to fetch-------------------------");
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

	        } catch(RekognitionException | InterruptedException e) {
	        	logger.log(e.getMessage());
	        }
			rekClient.close();
		} catch (Exception e) {
			logger.log("Exception:" + e.getMessage());
		}
		return "OK";
	}

}
