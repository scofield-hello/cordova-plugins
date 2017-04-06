package com.bolu.plugins.encryp;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Encoder {

  private String salt;

  public MD5Encoder(String salt){
    this.salt = salt;
  }



  public String encrypt(String rawPass) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    String saltedPass = mergePasswordAndSalt(rawPass, salt, false);
    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
    byte[] digest = messageDigest.digest(saltedPass.getBytes("UTF-8"));
    return getString(digest);
  }

  private  String getString(byte[] b) {
    StringBuffer strBuf = new StringBuffer();
    for (int i = 0; i < b.length; i++) {
      if (Integer.toHexString(0xff & b[i]).length() == 1) {
        strBuf.append("0").append(Integer.toHexString(0xff & b[i]));
      } else {
        strBuf.append(Integer.toHexString(0xff & b[i]));
      }
    }
    return strBuf.toString();
  }

  public String mergePasswordAndSalt(String password, Object salt, boolean strict) {
    if (password == null) {
      password = "";
    }
    if (strict && (salt != null)) {
      if ((salt.toString().lastIndexOf("{") != -1)
        || (salt.toString().lastIndexOf("}") != -1)) {
        throw new IllegalArgumentException(
          "Cannot use { or } in salt.toString()");
      }
    }
    if ((salt == null) || "".equals(salt)) {
      return password;
    } else {
      return password + "{" + salt.toString() + "}";
    }
  }

  public String encrypt(String source, int length) {
    String encrypted = source;
    try {
      for (int i = 0; i <= length; i++) {
        encrypted = encrypt(encrypted);
      }
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return encrypted;
  }
}
