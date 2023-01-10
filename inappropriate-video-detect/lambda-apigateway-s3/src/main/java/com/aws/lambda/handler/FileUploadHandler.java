package com.aws.lambda.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.MultipartStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.aws.lambda.dao.VideoDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileUploadHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {

		LambdaLogger logger = context.getLogger();

		logger.log("step1 - get requests info from APIgateway HTTP--------");
		logger.log("file body size: " + String.valueOf(event.getBody().getBytes().length));
		
		String clientRegion = "us-east-1";
		String bucketName = "rekognition-videos-bucket";
		String fileObjKeyName = "default.mp4";
		String fileObjHashName = "default.mp4";
		String fileObjHash = "default";

		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		try {
			byte[] bI = Base64.getDecoder().decode(event.getBody().getBytes());
			Map<String, String> hps = event.getHeaders();
			String contentType = "";
			if (hps != null) {
				contentType = hps.get("Content-Type");
			}
			String[] boundaryArray = contentType.split("=");
			byte[] boundary = boundaryArray[1].getBytes();
			ByteArrayInputStream content = new ByteArrayInputStream(bI);
			MultipartStream multipartStream = new MultipartStream(content, boundary, bI.length, null);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			boolean nextPart = multipartStream.skipPreamble();
			while (nextPart) {
				String header = multipartStream.readHeaders();
				if (!header.isEmpty()) {
					int index = header.indexOf("filename=");
					int secondIndex = header.indexOf(".mp4");
					if (index > 0 && secondIndex > 0) {
						fileObjKeyName = header.substring(index + 10, secondIndex);
						logger.log("Get original file name:" + fileObjKeyName);
						fileObjHash = generateNameOnContent(fileObjKeyName);
						fileObjHashName = fileObjHash + ".mp4";
						logger.log("Get hash key based on content:" + fileObjHashName);
					}
				}
				multipartStream.readBodyData(out);
				nextPart = multipartStream.readBoundary();
			}
			logger.log("step2 - generate outputstream from video file---------");
			InputStream fis = new ByteArrayInputStream(out.toByteArray());
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion).build();
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(out.toByteArray().length);
			metadata.setContentType("video/mp4");
			metadata.setCacheControl("public, max-age=31536000");
			s3Client.putObject(bucketName, fileObjHashName, fis, metadata);
			logger.log("step3 - save the video file to S3---------------------");
			
			VideoDO item = new VideoDO();
			item.setId(fileObjHash);
			item.save(item);
			logger.log("step4 - init basic video hash key info in DynamoDB-----");
			
			response.setIsBase64Encoded(false);
			response.setStatusCode(200);
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put("X-Powered-By", "AWS Lambda & Serverless");
			headers.put("Content-Type", "application/json");
			response.setHeaders(headers);
			HashMap<String, String> body = new HashMap<String, String>();
			body.put("status", "SUCCESS");
			body.put("hashID", item.getId());
			response.setBody(gson.toJson(body));
			logger.log("step5 - generate HTTP response -------------------------");
		} catch (Exception e) {
			logger.log("Exception: " + e.getMessage());
		}
		return response;

	}
	
	/**
	 * based on the content, generate the UUID for video
	 * @param originalName
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private String generateNameOnContent(String originalName) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.update(originalName.getBytes(), 0, originalName.length());
		return new BigInteger(1, m.digest()).toString(16);
	}
}
