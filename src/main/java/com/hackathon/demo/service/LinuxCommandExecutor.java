package com.hackathon.demo.service;

import com.jcraft.jsch.ChannelShell;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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


@Service
public class LinuxCommandExecutor {
    @Value("${remote.server.hostname}")
    private String SSH_HOST;
    @Value("${remote.server.user}")
    private String SSH_LOGIN;
    @Value("${remote.server.password}")
    private String SSH_PASSWORD;
            
    public boolean executeCommand(String command) {
        boolean result = true;
        Process process = null;
        try {
            System.out.println("Executing command "+ command);
            process = Runtime.getRuntime().exec(command); // for Linux
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                result = true;
                System.out.println("Command executed successfully");
            }
        } catch (Exception e) {
            System.out.println("Exception occured while executing command: "+ e);
            result = false;
        } finally {
            if (process !=null) {
                process.destroy();
            }
            
        }
        return result;
    }

    public Boolean runCommandOnRemoteServer(String command) {
        System.out.println("Run Command started SSH_HOST" + SSH_HOST + " SSH_LOGIN:"+SSH_LOGIN + " password: "+SSH_PASSWORD);
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
    
    
    public void runMutipleCommandsOnRemoteServer(String command, String directoryPath) throws  Exception{
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

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            channel = (ChannelShell) session.openChannel("shell");
            channel.setOutputStream(outputStream);
            PrintStream stream = new PrintStream(channel.getOutputStream());
            channel.connect();

            stream.println("cd "+directoryPath);
            stream.flush();
            String response = waitForPrompt(outputStream, "$");
            System.out.println(response);
            
            stream.println(command);
            stream.flush();
            response = waitForPrompt(outputStream, "$");
            System.out.println(response);

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    static public String waitForPrompt(ByteArrayOutputStream outputStream, String prompt) throws Exception {
        int retries = 2;
        for (int x = 1; x < retries; x++) {
            TimeUnit.SECONDS.sleep(1);
            if (outputStream.toString().indexOf(prompt) > 0) {
                String responseString = outputStream.toString();
                outputStream.reset();
                return responseString;
            }
        }
        throw new Exception("Prompt failed to show after specified timeout");
    }
    
}
