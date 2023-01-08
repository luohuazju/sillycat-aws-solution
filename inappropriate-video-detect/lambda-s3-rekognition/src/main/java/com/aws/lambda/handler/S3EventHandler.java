package com.aws.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.NotificationChannel;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.StartContentModerationRequest;
import software.amazon.awssdk.services.rekognition.model.StartContentModerationResponse;
import software.amazon.awssdk.services.rekognition.model.Video;

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
			
			
			rekClient.close();
		} catch (Exception e) {
			logger.log("Exception:" + e.getMessage());
		}
		return "OK";
	}

}
