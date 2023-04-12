package com.aws.javasdkexample.service;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest
@ActiveProfiles("dev")
public class S3ServiceTest {

	@Autowired
	S3Service s3Service;

	@Test
	public void dummy() {
		Assert.isTrue(true, "dummy assertion!");
	}

	@Test
	public void lastModified() {
		Date date = s3Service.getFileLastUpdate("rekognition-videos-bucket", "f6003dedc86b32bf6260ecefcbec4d00.mp4");
		Assert.notNull(date, "last date fetched!");
	}

}
