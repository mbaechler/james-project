package org.apache.james.blob.objectstorage.aws;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.BlobStoreContract;
import org.apache.james.blob.api.BucketName;
import org.apache.james.blob.api.HashBlobId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DockerAwsS3Extension.class)
class S3BlobStoreTest implements BlobStoreContract {

    private S3BlobStore testee;
    private S3DumbBlobStore s3DumbBlobStore;

    @BeforeEach
    void setUp(DockerAwsS3Container dockerAwsS3) {

        AwsS3AuthConfiguration configuration = AwsS3AuthConfiguration.builder()
            .endpoint(dockerAwsS3.getEndpoint())
            .accessKeyId(DockerAwsS3Container.ACCESS_KEY_ID)
            .secretKey(DockerAwsS3Container.SECRET_ACCESS_KEY)
            .build();

        s3DumbBlobStore = new S3DumbBlobStore(configuration, DockerAwsS3Container.REGION);
        testee = new S3BlobStore(s3DumbBlobStore, new HashBlobId.Factory(), BucketName.DEFAULT);
    }

    @AfterEach
    void tearDown() {
        s3DumbBlobStore.deleteAllBuckets().block();
        s3DumbBlobStore.close();
    }

    @Override
    public BlobStore testee() {
        return testee;
    }

    @Override
    public BlobId.Factory blobIdFactory() {
        return new HashBlobId.Factory();
    }

}