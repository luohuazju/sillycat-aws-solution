package com.aws.lambda.handler;

import java.util.HashMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileUploadHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {

		LambdaLogger logger = context.getLogger();
		logger.log("event:" + event);
		logger.log("event json: " + gson.toJson(event));

		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		response.setIsBase64Encoded(false);
		response.setStatusCode(200);
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Powered-By", "AWS Lambda & Serverless");
		headers.put("Content-Type", "application/json");
		response.setHeaders(headers);
		response.setBody(gson.toJson("OK"));
		return response;

	}
}
