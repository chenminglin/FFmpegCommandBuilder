package com.bethena.ffmpegcmdbuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * FFmpeg命令构建器
 * 使用构建者模式来构建FFmpeg命令
 */
public class FFmpegCommandBuilder {
    private final List<String> commands;

    private FFmpegCommandBuilder() {
        commands = new ArrayList<>();
        commands.add("-y"); // 默认覆盖输出文件
    }

    public static FFmpegCommandBuilder create() {
        return new FFmpegCommandBuilder();
    }

    /**
     * 设置输入文件
     *
     * @param inputPath 输入文件路径
     * @return 构建器实例
     */
    public FFmpegCommandBuilder input(String inputPath) {
        commands.add("-i");
        commands.add(inputPath);
        return this;
    }

    /**
     * 设置视频编码器
     *
     * @param codec 编码器名称（如 libx264, h264_mediacodec 等）
     * @return 构建器实例
     */
    public FFmpegCommandBuilder videoCodec(String codec) {
        commands.add("-c:v");
        commands.add(codec);
        return this;
    }

    /**
     * 设置音频编码器
     *
     * @param codec 编码器名称（如 aac, mp3 等）
     * @return 构建器实例
     */
    public FFmpegCommandBuilder audioCodec(String codec) {
        commands.add("-c:a");
        commands.add(codec);
        return this;
    }

    /**
     * 设置视频比特率
     *
     * @param bitrate 比特率（如 "2M", "500k" 等）
     * @return 构建器实例
     */
    public FFmpegCommandBuilder videoBitRate(String bitrate) {
        commands.add("-b:v");
        commands.add(bitrate);
        return this;
    }

    /**
     * 设置音频比特率
     *
     * @param bitrate 比特率（如 "128k", "192k" 等）
     * @return 构建器实例
     */
    public FFmpegCommandBuilder audioBitRate(String bitrate) {
        commands.add("-b:a");
        commands.add(bitrate);
        return this;
    }

    /**
     * 设置视频分辨率
     *
     * @param width  宽度
     * @param height 高度
     * @return 构建器实例
     */
    public FFmpegCommandBuilder resolution(int width, int height) {
        commands.add("-s");
        commands.add(width + "x" + height);
        return this;
    }

    /**
     * 设置视频帧率
     *
     * @param fps 帧率
     * @return 构建器实例
     */
    public FFmpegCommandBuilder frameRate(int fps) {
        commands.add("-r");
        commands.add(String.valueOf(fps));
        return this;
    }

    /**
     * 裁剪视频时长
     *
     * @param duration 持续时间（格式：HH:mm:ss）
     * @return 构建器实例
     */
    public FFmpegCommandBuilder duration(String duration) {
        commands.add("-t");
        commands.add(duration);
        return this;
    }

    /**
     * 设置输出文件
     *
     * @param outputPath 输出文件路径
     * @return 构建器实例
     */
    public FFmpegCommandBuilder output(String outputPath) {
        commands.add(outputPath);
        return this;
    }

    /**
     * 构建最终的FFmpeg命令字符串
     *
     * @return FFmpeg命令字符串
     */
    public String build() {
        return String.join(" ", commands);
    }
}