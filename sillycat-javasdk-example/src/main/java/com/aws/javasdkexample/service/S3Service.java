package com.aws.javasdkexample.service;

import java.util.Date;

public interface S3Service {

	public Date getFileLastUpdate(String bucketName, String path);

}
