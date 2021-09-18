package jFaaS.invokers;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;

public class AsyncLambdaInvoker implements FaaSInvoker{

    private String awsSessionToken;
    private String awsAccessKey;
    private String awsSecretKey;
    private AWSLambda lambda;

    /**
     * Basic Constructor that creates an LambdaInvoker for a specific region with standard settings.
     *
     * @param awsAccessKey aws access key
     * @param awsSecretKey aws secret key
     * @param region       of the cloud function
     */
    public AsyncLambdaInvoker(String awsAccessKey, String awsSecretKey, String awsSessionToken, Regions region) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.awsSessionToken = awsSessionToken;
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(1);
        clientConfiguration.setSocketTimeout(900 * 1000);
        clientConfiguration.setMaxConnections(10000);

        if(awsSessionToken != null) {
            BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                    awsAccessKey,
                    awsSecretKey,
                    awsSessionToken);
            this.lambda = AWSLambdaClientBuilder.standard().withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                    .withClientConfiguration(clientConfiguration)
                    .build();
        } else {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
            this.lambda = AWSLambdaClientBuilder.standard().withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withClientConfiguration(clientConfiguration)
                    .build();
        }
    }

    /**
     * Constructor that creates an LambdaInvoker for a specific region with custom ClientConfiguration.
     *
     * @param awsAccessKey        aws access key
     * @param awsSecretKey        aws secret key
     * @param region              of the cloud function
     * @param clientConfiguration custom client configuration
     */
    public AsyncLambdaInvoker(String awsAccessKey, String awsSecretKey, Regions region, ClientConfiguration clientConfiguration) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        this.lambda = AWSLambdaClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withClientConfiguration(clientConfiguration)
                .build();
    }

    /**
     * Invokes the lambda function.
     *
     * @param function       function name or ARN
     * @param functionInputs inputs of the function to invoke
     * @return json result
     */
    public JsonObject invokeFunction(String function, Map<String, Object> functionInputs) throws IOException {
        String payload = new Gson().toJson(functionInputs);
        InvokeRequest invokeRequest = new InvokeRequest().withFunctionName(function)
                .withInvocationType(InvocationType.Event).withPayload(payload);

        InvokeResult invokeResult = this.lambda.invoke(invokeRequest);

        assert invokeResult != null;

        Integer statusCode = invokeResult.getStatusCode();
        if(statusCode == 202)
            return new Gson().fromJson("{\"response\":\"function started\",\"statusCode\":"+statusCode+"}",JsonObject.class);
        return new Gson().fromJson("{\"response\":\"an error occured\",\"statusCode\":"+statusCode+"}", JsonObject.class);
    }
}