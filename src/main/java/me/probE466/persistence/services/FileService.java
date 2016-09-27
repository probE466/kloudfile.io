package me.probE466.persistence.services;


import me.probE466.persistence.entities.File;
import me.probE466.persistence.entities.User;
import me.probE466.persistence.repos.FileRepository;
import me.probE466.persistence.repos.UserRepository;
import org.apache.tomcat.jni.Buffer;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityExistsException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class FileService {


    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    private final java.io.File fileDir = new java.io.File(System.getProperty("user.home") + "/push/files");
    private final java.io.File imageDir = new java.io.File(System.getProperty("user.home") + "/push/images");

    public FileRepository getFileRepository() {
        return fileRepository;
    }

    public boolean createFile(InputStream fsin, final String fileName, User user) {
        File dstFile = new File();
        String hash = "";
        try {
            hash = calcSHA2(fsin);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (checkIfFileExists(hash)) {
            throw new EntityExistsException();
        }
        dstFile.setIsImage(isImage(fileName));
        dstFile.setFileHash(hash);
        dstFile.setFileName(fileName);
        dstFile.setFilePath(saveFile(fsin, fileName, dstFile.getIsImage()));
        dstFile.setUserId(user);
        fileRepository.save(dstFile);
        user.getFileList().add(dstFile);
        userRepository.save(user);
        return true;
    }

    private boolean isImage(final String fileName) {
        String[] strArr = fileName.split("\\.");
        if (strArr.length <= 1) {
            throw new UnsupportedOperationException();
        }
        String ext = strArr[strArr.length - 1];
        return ext.equals("png") || ext.equals("jpg") || ext.equals("bmp") || ext.equals("gif");
    }

    private String saveFile(InputStream fsin, String fileName, boolean isImage) {
        java.io.File svFile;
        fileName = fileName.replaceAll("\\s+","");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }
        if (isImage) {
            svFile = new java.io.File(imageDir.getPath(), fileName);
        } else {
            svFile = new java.io.File(fileDir.getPath(), fileName);
        }
        try {
            svFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream fout = new FileOutputStream(svFile)) {
            IOUtils.copy(fsin, fout);
            fout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return svFile.getPath();
    }

    private boolean checkIfFileExists(final String hash) {
        return fileRepository.findByFileHash(hash).isPresent();
//        return fileRepository.findAll().stream().anyMatch(file -> file.getHash().equals(hash));
    }

    private String calcSHA2(final InputStream input) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha2 = MessageDigest.getInstance("SHA-256");
        BufferedInputStream buf = new BufferedInputStream(input);
        buf.mark(input.available());
        byte[] buffer = new byte[8192];
        int len = input.read(buffer);

        while (len != -1) {
            sha2.update(buffer, 0, len);
            len = input.read(buffer);
        }

        buf.reset();
        return new HexBinaryAdapter().marshal(sha2.digest());
    }

}
