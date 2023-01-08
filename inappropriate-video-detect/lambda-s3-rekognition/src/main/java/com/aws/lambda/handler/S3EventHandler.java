package com.aws.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import software.amazon.awssdk.services.rekognition.RekognitionClient;

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
			
			RekognitionClient rekClient = RekognitionClient.builder().build();
			

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return "OK";
	}

}
