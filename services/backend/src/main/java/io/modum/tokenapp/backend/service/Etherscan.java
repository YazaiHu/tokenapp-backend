package io.modum.tokenapp.backend.service;


import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Service
public class Etherscan {
    @Value("${modum.token.etherscan:YourApiKeyToken}")
    private String apiKey;

    @Value("${modum.url.etherscan:rinkeby.etherscan.io}")
    private String url; //api.etherscan.io or rinkeby.etherscan.io

    @Autowired
    private RestTemplate restTemplate;

    public BigInteger getBalance(String address) {
        String s = "https://"+url+"/api" +
                "?module=account" +
                "&action=balance" +
                "&address=" + address +
                "&tag=latest" +
                "&apikey="+apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "the mighty tokenapp-backend");

        ReturnSingleValue retVal = restTemplate.exchange(s, HttpMethod.GET,new HttpEntity<>(null, headers), ReturnSingleValue.class).getBody();
        return new BigInteger(retVal.result);
    }

    /**
     * Can process up to 20 contracts
     */
    public BigInteger get20Balances(String... contract) {
        return get20Balances(Arrays.asList(contract));
    }

    /**
     * Can process up to 20 contracts
     */
    public BigInteger get20Balances(List<String> contract) {

        String addresses = String.join(",", contract);
        String s = "https://"+url+"/api" +
                "?module=account" +
                "&action=balancemulti" +
                "&address=" + addresses +
                "&tag=latest" +
                "&apikey="+apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "the mighty tokenapp-backend");

        ReturnValues retVal = restTemplate.exchange(s, HttpMethod.GET,new HttpEntity<>(null, headers), ReturnValues.class).getBody();
        BigInteger result = BigInteger.ZERO;
        for(ReturnValues.Result res: retVal.result) {
            result = result.add(new BigInteger(res.balance));
        }
        return result;
    }

    /**
     * This may take a while, make sure you obey the limits of the api provider
     */
    public BigInteger getBalances(List<String> contract) {
        BigInteger result = BigInteger.ZERO;
        List<List<String>> part = Lists.partition(contract, 20);
        for(List<String> p:part) {
            result = result.add(get20Balances(p));
        }
        return result;
        //TODO:caching!
    }

    private static class ReturnSingleValue {
        @JsonProperty("status")
        public String status;
        @JsonProperty("message")
        public String message;
        @JsonProperty("result")
        public String result;
    }

    private static class ReturnValues {
        @JsonProperty("status")
        public String status;
        @JsonProperty("message")
        public String message;
        @JsonProperty("result")
        public List<Result> result = null;
        public static class Result {
            @JsonProperty("account")
            public String account;
            @JsonProperty("balance")
            public String balance;
        }
    }
}
