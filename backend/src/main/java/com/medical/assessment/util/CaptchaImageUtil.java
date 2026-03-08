package com.medical.assessment.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * 简单图形验证码：生成随机字符图片（PNG），用于登录人机校验。
 */
public final class CaptchaImageUtil {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;
    private static final Random RANDOM = new Random();

    private CaptchaImageUtil() {}

    /**
     * 生成验证码与对应图片。
     *
     * @return [0]=验证码字符串, [1]=PNG 图片字节
     */
    public static Object[] generate() throws IOException {
        String code = randomCode(CODE_LEN);
        byte[] imageBytes = drawImage(code);
        return new Object[]{code, imageBytes};
    }

    private static String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private static byte[] drawImage(String code) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(RANDOM.nextInt(180), RANDOM.nextInt(180), RANDOM.nextInt(180)));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        g.setFont(new Font("Arial", Font.BOLD, 28));
        int x = 15;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RANDOM.nextInt(100) + 20, RANDOM.nextInt(100) + 20, RANDOM.nextInt(100) + 20));
            g.drawString(String.valueOf(code.charAt(i)), x, 28);
            x += 26;
        }

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
