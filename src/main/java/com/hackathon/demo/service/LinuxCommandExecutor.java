package com.hackathon.demo.service;

import com.hackathon.demo.controller.HackathonDemoController;
import com.jcraft.jsch.ChannelShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.io.CharStreams;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.web.client.RestTemplate;


@Service
public class LinuxCommandExecutor {
    Logger logger = LoggerFactory.getLogger(LinuxCommandExecutor.class);
    
    @Value("${remote.server.hostname}")
    private String SSH_HOST;
    @Value("${remote.server.user}")
    private String SSH_LOGIN;
    @Value("${remote.server.password}")
    private String SSH_PASSWORD;
    @Value("${remote.directory.path}")
    private String DIRECTORY_PATH;
    @Value("${async.operationTimeout}")
    private Integer asyncTimeOut;

    @Autowired
    private RestTemplate restTemplate;

    @Async
    public void executeCommandAsync(String command) {
        logger.info("LinuxCommandExecutor service: Async command: " + command);
        Future<Integer> future = CompletableFuture.supplyAsync(() -> {
                    int exitCode = 0;
                    String[] args = new String[]{"/bin/bash", "-c", command};
                    ProcessBuilder pb = new ProcessBuilder(args);
                    pb.directory(new File(DIRECTORY_PATH));
                    Process process = null;
                    try {
                        process = pb.start();
                    } catch (IOException e) {
                        logger.error("Error occurred while staring the process " + e);
                    }
                    logger.info("Command Async execution started");
                    try {
                        exitCode = process.waitFor();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            logger.info(line);
                        }

                        if (exitCode == 0) {
                            logger.info("Command executed successfully exitCode: "+exitCode);
                        } else {
                            logger.info("Error occurred while executing command exit code: "+exitCode);
                        }

                    } catch (InterruptedException | IOException e) {
                        logger.error("Error occurred while Running the command, exception: " + e);
                    }
                    return exitCode;
                }).thenApplyAsync(exitCode -> exitCode)
                .exceptionally(ex -> {
                    logger.error("Error occurred while executing command" + ex);
                    throw new RuntimeException("Error occurred while  executing command");
                });

        if (future != null) {
            try {
                logger.info("Exit code: " + future.get(asyncTimeOut, TimeUnit.MILLISECONDS));
            } catch (Exception exception) {
                logger.warn("No Data found in future object:" + exception.getMessage());
            }
        }
    }
    
    public boolean executeCommand(String command) {
        boolean result = true;
        Process process = null;
        try {
            logger.info("Command " + command);
            //     process = Runtime.getRuntime().exec(command); // for Linux

            //NEW code
            String[] args = new String[]{"/bin/bash", "-c", command};
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(new File(DIRECTORY_PATH));
            process = pb.start();
            logger.info("Command execution started");
            //end
            //   int exitCode = process.waitFor();
            boolean isSuccess = process.waitFor(2, TimeUnit.SECONDS);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
                logger.info("Command executed successfully");
            }
            logger.info("Is command is still executing " + !isSuccess);
//            if (exitCode == 0) {
//                result = true;
//                System.out.println("Command executed successfully"); 
//            } else {
//                System.out.println("Error occurred while executing command");
//                result = false;
//            }
        } catch (Exception e) {
            logger.error("Exception occurred while executing command: " + e);
            result = false;
        } finally {
            if (process != null) {
                process.destroy();
            }

        }
        return result;
    }


    public Boolean runCommandOnRemoteServer(String command) {
        logger.info("Run Command started SSH_HOST" + SSH_HOST + " User:" + SSH_LOGIN + " password: " + SSH_PASSWORD);
        boolean result = true;

        Session session = setupSshSession();
        try {
            session.connect();
        } catch (JSchException e) {
            logger.info("Exception occurred while connecting to remote server: " + e);
        }

        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
        } catch (JSchException e) {
            logger.info("Exception occurred while connecting to remote server with open channel: " + e);
            result = false;
        }
        try {
            if (channel == null) {
                logger.info("Not able to connect with remote server with Host " + SSH_HOST + " User:" + SSH_LOGIN + " password:" + SSH_PASSWORD);
                return false;
            }
            channel.setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();
            channel.connect();


            String response = CharStreams.toString(new InputStreamReader(output));
            logger.info("Command executed successfully Result " + response);

        } catch (JSchException | IOException e) {
            logger.error("Exception occurred while running the command " + e);
            result = false;
            closeConnection(channel, session);
            throw new RuntimeException(e);

        } finally {
            if (channel != null && session != null) {
                closeConnection(channel, session);
            }
        }
        return result;
    }

    private Session setupSshSession() {
        Session session = null;
        try {
            session = new JSch().getSession(SSH_LOGIN, SSH_HOST, 22);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        session.setPassword(SSH_PASSWORD);
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    private static void closeConnection(ChannelExec channel, Session session) {
        try {
            channel.disconnect();
        } catch (Exception ignored) {
        }
        session.disconnect();
    }


    public String runMutipleCommandsOnRemoteServer(String command, String directoryPath) throws Exception {
        System.out.println("Run Command started SSH_HOST" + SSH_HOST + " User:" + SSH_LOGIN + " password: " + SSH_PASSWORD);

        String response = null;
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        Session session = null;
        ChannelShell channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(SSH_LOGIN, SSH_HOST, 22);
            session.setConfig(config);
            session.setPassword(SSH_PASSWORD);
            session.connect();
            System.out.println("Connected to the remote server....");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            channel = (ChannelShell) session.openChannel("shell");
            channel.setOutputStream(outputStream);
            PrintStream stream = new PrintStream(channel.getOutputStream());
            channel.connect();

            if (directoryPath != null && directoryPath != "") {
                stream.println("cd " + directoryPath);
                stream.flush();
                response = waitForPrompt(outputStream, "$");
            }

            stream.println(command);
            stream.flush();
            System.out.println("Waiting for the response for the command....");
            response = waitForPrompt(outputStream, "$");

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        return response;
    }

    static public String waitForPrompt(ByteArrayOutputStream outputStream, String prompt) throws Exception {
        int retries = 3;
        for (int x = 1; x < retries; x++) {
            TimeUnit.SECONDS.sleep(20);
            if (outputStream.toString().indexOf(prompt) > 0) {
                String responseString = outputStream.toString();
                outputStream.reset();
                return responseString;
            }
        }
        throw new Exception("Prompt failed to show after specified timeout");
    }


    public String getMetricsData(String metricsURI) {
        String response = null;
        ResponseEntity<String> responseData
                = restTemplate.getForEntity(metricsURI, String.class);
        if (responseData != null) {
            response = responseData.getBody();
        }
        return response;
    }
}
