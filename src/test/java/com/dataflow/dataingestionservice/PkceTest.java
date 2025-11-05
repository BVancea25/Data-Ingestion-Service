package com.dataflow.dataingestionservice;

import com.dataflow.dataingestionservice.bt.util.PkceUtil;

public class PkceTest {
    public static void main(String[] args) {
        String verifier = PkceUtil.generateCodeVerifier();
        String challenge = PkceUtil.generateCodeChallenge(verifier);
        System.out.println("Code Verifier: " + verifier);
        System.out.println("Code Challenge: " + challenge);
    }
}
