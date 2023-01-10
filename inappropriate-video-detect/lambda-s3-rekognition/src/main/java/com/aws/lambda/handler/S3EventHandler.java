package com.aws.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.aws.lambda.dao.VideoDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.NotificationChannel;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.StartContentModerationRequest;
import software.amazon.awssdk.services.rekognition.model.StartContentModerationResponse;
import software.amazon.awssdk.services.rekognition.model.Video;

/**
 * s3 events trigger lambda to start rekognition tasks on video files
 * @author Carl
 *
 */
public class S3EventHandler implements RequestHandler<S3Event, String> {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public String handleRequest(S3Event event, Context context) {
		LambdaLogger logger = context.getLogger();

		logger.log("step1 - receive s3 event the file created ---------");
		logger.log("event json content: " + gson.toJson(event));
		try {
			// Get Event Record
			S3EventNotificationRecord record = event.getRecords().get(0);
			// Source File Name
			String srcFileName = record.getS3().getObject().getKey();
			logger.log("parse and get file name: " + srcFileName);
			String id = srcFileName.substring(0,srcFileName.length() - 4);
			logger.log("parse and get hash key: " + id);
			String snsArn = System.getenv("rekognitionSNSArn");
			logger.log("SNS topic ARN:" + snsArn);
			String roleArn = System.getenv("roleArn");
			logger.log("SNS topic roleArn:" + roleArn);

			RekognitionClient rekClient = RekognitionClient.builder().build();
			NotificationChannel channel = NotificationChannel.builder().snsTopicArn(snsArn).roleArn(roleArn).build();
			S3Object s3Obj = S3Object.builder().bucket("rekognition-videos-bucket").name(srcFileName).build();
			Video vidOb = Video.builder().s3Object(s3Obj).build();
			StartContentModerationRequest modDetectionRequest = StartContentModerationRequest.builder()
					.jobTag("Moderation").notificationChannel(channel).video(vidOb).build();
			StartContentModerationResponse startModDetectionResult = rekClient
					.startContentModeration(modDetectionRequest);
			logger.log("step2 - trigger rekognition to work on video-----");
			String startJobId = startModDetectionResult.jobId();
			logger.log("rekognition response job ID:" + startJobId);
			
			VideoDO item = new VideoDO();
			item.setId(id);
			item.setJobID(startJobId);
			item.save(item);
			logger.log("step3 - update jobID in dynamoDB-----------------");
			rekClient.close();
		} catch (Exception e) {
			logger.log("Exception:" + e.getMessage());
		}
		return "OK";
	}

}
