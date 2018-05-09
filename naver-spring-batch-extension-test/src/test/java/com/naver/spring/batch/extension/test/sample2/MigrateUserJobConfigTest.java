package com.naver.spring.batch.extension.test.sample2;

import com.naver.spring.batch.extension.test.sample.TestConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author fomuo@navercorp.com
 */
@RunWith(SpringRunner.class)
@Import({TestConfig.class, MigrateUserJobConfig.class })
@SpringBootTest(classes = { TestConfig.class, MigrateUserJobConfig.class })
public class MigrateUserJobConfigTest {
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void migrateUserJob() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob();
		Assert.assertEquals(jobExecution.getStatus(), BatchStatus.COMPLETED);
	}
}
