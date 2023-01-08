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

		return "OK";
	}

}
