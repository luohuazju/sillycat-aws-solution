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
import com.aws.lambda.dao.Video;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileUploadHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private String generateNameOnContent(String originalName) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.update(originalName.getBytes(), 0, originalName.length());
		return new BigInteger(1, m.digest()).toString(16);
	}

	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {

		LambdaLogger logger = context.getLogger();
		// logger.log("event:" + event);

		// logger.log("event json: " + gson.toJson(event));

		logger.log("1 Incoming body size: " + String.valueOf(event.getBody().getBytes().length));
		String clientRegion = "us-east-1";
		String bucketName = "rekognition-videos-bucket";
		String fileObjKeyName = "video_sample.mp4";
		String fileObjHashName = "default.mp4";
		String fileObjHash = "default";

		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();

		try {
			// Get the uploaded file and decode from base64
			byte[] bI = Base64.getDecoder().decode(event.getBody().getBytes());
			logger.log("2 Decode the base64 from body");

			// Get the content-type header
			Map<String, String> hps = event.getHeaders();
			String contentType = "";
			if (hps != null) {
				logger.log("2.1 get content type from map");
				contentType = hps.get("Content-Type");

			}
			logger.log("3 get content type:" + contentType);

			// Extract the boundary
			String[] boundaryArray = contentType.split("=");

			// Transform the boundary to a byte array
			byte[] boundary = boundaryArray[1].getBytes();

			// Log the extraction for verification purposes
			// logger.log("3.1 " + new String(bI, "UTF-8") + "\n");

			logger.log("4 get boundary done");

			// Create a ByteArrayInputStream
			ByteArrayInputStream content = new ByteArrayInputStream(bI);

			// Create a MultipartStream to process the form-data
			MultipartStream multipartStream = new MultipartStream(content, boundary, bI.length, null);

			// Create a ByteArrayOutputStream
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			// Find first boundary in the MultipartStream
			boolean nextPart = multipartStream.skipPreamble();

			logger.log("5 prepare the data");

			// Loop through each segment
			while (nextPart) {
				String header = multipartStream.readHeaders();

				// Log header for debugging
				logger.log("5.1 Headers:" + header);
				if (!header.isEmpty()) {
					int index = header.indexOf("filename=");
					int secondIndex = header.indexOf(".mp4");
					if (index > 0 && secondIndex > 0) {
						fileObjKeyName = header.substring(index + 10, secondIndex);
						logger.log("5.2 fileObjKeyName:" + fileObjKeyName);
						fileObjHash = generateNameOnContent(fileObjKeyName);
						fileObjHashName = fileObjHash + ".mp4";
						logger.log("5.3 fileObjHashName:" + fileObjHashName);
					}
				}

				// Write out the file to our ByteArrayOutputStream
				multipartStream.readBodyData(out);

				// Get the next part, if any
				nextPart = multipartStream.readBoundary();

			}

			// Log completion of MultipartStream processing
			logger.log("6 Data written to ByteStream");

			// Prepare an InputStream from the ByteArrayOutputStream
			InputStream fis = new ByteArrayInputStream(out.toByteArray());

			// Create our S3Client Object
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion).build();

			logger.log("7 prepare s3 client-----");
			// Configure the file metadata
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(out.toByteArray().length);
			metadata.setContentType("video/mp4");
			metadata.setCacheControl("public, max-age=31536000");

			// Put file into S3
			s3Client.putObject(bucketName, fileObjHashName, fis, metadata);

			// Log status
			logger.log("8 Put object in S3 done");

			logger.log("meta information to dynamodb");
			Video item = new Video();
			item.setId(fileObjHash);
			item.save(item);

			response.setIsBase64Encoded(false);
			response.setStatusCode(200);
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put("X-Powered-By", "AWS Lambda & Serverless");
			headers.put("Content-Type", "application/json");
			response.setHeaders(headers);
			response.setBody(gson.toJson(item.getId()));

		} catch (Exception e) {
			logger.log("Exception: " + e.getMessage());
		}

		return response;

	}
}
