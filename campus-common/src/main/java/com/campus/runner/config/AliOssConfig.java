package com.campus.runner.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.CreateBucketRequest;
import com.campus.runner.properties.AliOssProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AliOssProperties.class)
public class AliOssConfig {

    @Bean
    public OSS createOssClient(AliOssProperties aliOssProperties) {
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        DefaultCredentialProvider credentialProvider = CredentialsProviderFactory.newDefaultCredentialProvider(aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret());

        OSS ossClient = OSSClientBuilder.create()
                .endpoint(aliOssProperties.getEndpoint())
                .region(aliOssProperties.getRegion())
                .credentialsProvider(credentialProvider)
                .build();

        // 演示降级固化：bucket 探活/创建是网络调用，未配置真实 OSS（placeholder）或离线环境时
        // 会因 DNS 解析失败抛异常炸掉整个应用启动。此处仅告警降级——上传接口在请求期
        // 各自失败并提示"文件上传失败"，不影响登录/交易等核心链路。
        try {
            String bucketName = aliOssProperties.getBucketName();
            boolean exist = ossClient.doesBucketExist(bucketName);
            if (!exist) {
                // 如果不存在则创建存储空间
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
                ossClient.createBucket(createBucketRequest);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AliOssConfig.class)
                    .warn("[演示降级]OSS 初始化探活失败（bucket 检查已跳过），文件上传将不可用，其余功能不受影响：{}", e.getMessage());
        }

        return ossClient;
    }
}
