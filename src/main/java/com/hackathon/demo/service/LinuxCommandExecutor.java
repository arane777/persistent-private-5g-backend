package com.hackathon.demo.service;

import com.jcraft.jsch.ChannelShell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import com.google.common.io.CharStreams;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.web.client.RestTemplate;


@Service
public class LinuxCommandExecutor {
    @Value("${remote.server.hostname}")
    private String SSH_HOST;
    @Value("${remote.server.user}")
    private String SSH_LOGIN;
    @Value("${remote.server.password}")
    private String SSH_PASSWORD;
    @Value("${remote.directory.path}")
    private String DIRECTORY_PATH;

    
    @Autowired
    private RestTemplate restTemplate;
            
    public boolean executeCommand(String command) {
        boolean result = true;
        Process process = null;
        try {
            System.out.println("Executing command "+ command);
       //     process = Runtime.getRuntime().exec(command); // for Linux
            
            //NEW code
            String[] args = new String[] {"/bin/bash", "-c",command};
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(new File(DIRECTORY_PATH));
            pb.start();
            System.out.println("Command executed successfully"); 
            //end
//            process.waitFor();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//                result = true;
//                System.out.println("Command executed successfully");
//            }
        } catch (Exception e) {
            System.out.println("Exception occurred while executing command: "+ e);
            result = false;
        } finally {
            if (process !=null) {
                process.destroy();
            }
            
        }
        return result;
    }


    public Boolean runCommandOnRemoteServer(String command) {
        System.out.println("Run Command started SSH_HOST" + SSH_HOST + " User:"+SSH_LOGIN + " password: "+SSH_PASSWORD);
        boolean result = true;
        
        Session session = setupSshSession();
        try {
            session.connect();
        } catch (JSchException e) {
            System.out.println("Exception occurred while connecting to remote server: "+ e);
        }

        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
        } catch (JSchException e) {
            System.out.println("Exception occurred while connecting to remote server with open channel: "+ e);
            result = false;
        }
        try {
            if (channel == null) {
                System.out.println("Not able to connect with remote server with Host "+ SSH_HOST + " User:"+SSH_LOGIN + " password:"+ SSH_PASSWORD); 
                return false;
            }
            channel.setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();
            channel.connect();
            
            
            String response = CharStreams.toString(new InputStreamReader(output));
            System.out.println("Command executed successfully Result " +response);

        } catch (JSchException | IOException e) {
            System.out.println("Exception occurred while running the command "+ e);
            result = false;
            closeConnection(channel, session);
            throw new RuntimeException(e);

        } finally {
            if (channel != null && session!=null){
                closeConnection(channel, session);  
            }
        }
        return  result;
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
    
    
    public String runMutipleCommandsOnRemoteServer(String command, String directoryPath) throws  Exception{
        System.out.println("Run Command started SSH_HOST" + SSH_HOST + " User:"+SSH_LOGIN + " password: "+SSH_PASSWORD);

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

            if (directoryPath != null && directoryPath!= "") {
                stream.println("cd "+directoryPath);
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
                = restTemplate.getForEntity(metricsURI , String.class);
        if (responseData != null ) {
            response = responseData.getBody();
        } 
        return response;
    }
}
