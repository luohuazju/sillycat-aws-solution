package com.aws.javasdkexample.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Service
public class S3ServiceImpl implements S3Service {
	
	private static final Logger logger = LoggerFactory.getLogger(S3ServiceImpl.class);

	private AmazonS3 s3Client;

	private void init() {
		logger.info("start to init the s3Client--------");
		this.s3Client = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(
						new ProfileCredentialsProvider("default").getCredentials()))
				.withRegion(Regions.US_EAST_1).build();
		logger.info("start to init the s3Client end--------");
	}

	public Date getFileLastUpdate(String bucketName, String path) {
		if (this.s3Client == null) {
			this.init();
		}
		Date result = null;
		ObjectListing objectList = this.s3Client.listObjects(bucketName, path);
		if (objectList != null) {
			for (S3ObjectSummary fileSummary : objectList.getObjectSummaries()) {
				String tmpKey = fileSummary.getKey();
				logger.info("find files {}", tmpKey);
				logger.info("file the file with last update {}", fileSummary.getLastModified());
				result = fileSummary.getLastModified();
				break;
			}
		}
		return result;
	}


}
