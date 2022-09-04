package com.study;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Typora中未引用图片清理
 *
 * @author zhang
 */
public class PictureClear {
    private final static Logger logger = LoggerFactory.getLogger(PictureClear.class);

    /**
     * 这个列表是有图片的markdown文件
     */
    static Set<String> markdownNameSet = new HashSet<>();

    /**
     * 所有markdown里面所有的图片
     */
    static Set<String> pictureNameInMarkdown = new HashSet<>();

    static List<String> targetPath = new ArrayList<>();

    /**
     * 文件分割符号
     */
    static String separator = File.separator;

    /**
     * url编码格式
     */
    static String encoding = "UTF-8";

    /**
     * 图床路径 public static String figureBedPath = "D:\\NoteBook\\docker学习\\image";
     */
    public static String figureBedPath ;

    /**
     * 删除目录
     * public static String toDeletePath = "D:\\NoteBook\\docker学习\\toDelete";
     */
    public static String toDeletePath;


    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        // 初始化参数
        init(args);
        //获取带图片的md文件列表
        doSearchBandPicMarkdown();
        printFileList(pictureNameInMarkdown, "markwodn");
        //所有的图片
        Set<String> allPictures = getFigureBedFileList();
        printFileList(allPictures, "image");
        // 将不匹配的图片重命名并且挪到 toDelete文件夹里面
        doReName(pictureNameInMarkdown, allPictures);
        logger.info("完成时间: {} ms", (System.currentTimeMillis() - start));
    }

    /**
     * 初始化图床路径
     * args[0]: 图床路径
     * args[1]: 临时路径，待删除图片
     * args[2]: 待扫描路径, 可用多个, 中间用,分割
     *
     * @param args 控制台参数
     */
    private static void init(String[] args) {
        if (isEmptyArrays(args)) {
            logger.error("参数为空");
            logger.info("args[0]: 图床路径");
            logger.info("args[1]: 临时路径，待删除图片");
            logger.info("args[2]: 待扫描路径, 可用多个, 中间用,分割");
            System.exit(0);
        }
        // 赋值
        //图床路径
        PictureClear.figureBedPath = args[0];
        // 删除路径
        PictureClear.toDeletePath = args[1];
        String arg = args[2];
        String[] split = arg.split(",");
        for (String s : split) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            targetPath.add(s);
        }
        if (!new File(toDeletePath).exists()) {
            new File(toDeletePath).mkdirs();
        }
    }

    /**
     * 在Markdown文件中搜索图片
     *
     * @throws IOException IO异常
     */
    private static void doSearchBandPicMarkdown() throws IOException {
        logger.info("开始搜索markdown中的图片=================");
        for (String path : targetPath) {
            List<File> files = Arrays.stream(
                    Optional.ofNullable(new File(path).listFiles())
                            .orElse(new File[0]))
                    .distinct()
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(files)) {
                return;
            }
            doSearchFiles(files);
        }
        logger.info("搜索markdown中的图片结束=================");
    }


    private static void doSearchFiles(List<File> files) throws IOException {
        files = files.stream()
                .filter(file -> !file.isHidden())
                .collect(Collectors.toList());
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith("md")) {
                logger.info("检索到markdown文件 :" + file.getPath());
                doSetMarkdownNameList(file);
            } else {
//                if (file.isFile() && isImage(file.getName())) {
//                    logger.info("检索到image文件 :" + file.getPath());
//                }
                doRecursionSearchMdFile(file);
            }
        }
    }

    private static boolean isImage(String fileName) {
        return fileName.endsWith("jpg") ||
                fileName.endsWith("jpeg") ||
                fileName.endsWith("gif") ||
                fileName.endsWith("png");
    }

    /**
     * 递归搜索markdown文件
     *
     * @param file 文件路径
     * @throws IOException IO异常
     */
    private static void doRecursionSearchMdFile(File file) throws IOException {
        List<File> files = Arrays.stream(Optional.ofNullable(file.listFiles()).orElse(new File[0]))
                .collect(Collectors.toList());
        doSearchFiles(files);
    }

    private static void doSetMarkdownNameList(File file) throws IOException {
        String s1 = FileUtils.readFileToString(file, encoding);
        // 捕获组，匹配类似于 "![*](*)" 的字符串
        String regex = "(!\\[.*])(\\(.*\\))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s1);
        while (matcher.find()) {
            String ref = matcher.group(0);
            // 获取图片名称
            int beginIndex = ref.indexOf("](") + 2;
            int endIndex = ref.length() - 1;
            String pictureName = ref.substring(beginIndex, endIndex);
            pictureName = URLDecoder.decode(pictureName, encoding);
            // 保存图片名称
            if (pictureNameInMarkdown.add(pictureName)) {
                markdownNameSet.add(file.getPath());
            }
        }
    }

    /**
     * 获取图床文件列表, 排除掉 范围之外的.
     *
     * @return 剔除后的图床文件列表
     */
    private static Set<String> getFigureBedFileList() {
        logger.info("figureBedPath Name{}", figureBedPath);
        File file = new File(figureBedPath);
        logger.info("figureBedPath目录{}", file.getAbsolutePath());
        File[] files = file.listFiles();
        Set<String> result = new HashSet<>();
        if (isEmptyArrays(files)) {
            return result;
        }
        for (File file1 : files) {
            logger.info("目录{}", file1.getName());
            if (!file1.isDirectory()) {
                continue;
            }
            String path = file + separator + file1.getName();
            if (new File(path).compareTo(new File(toDeletePath)) == 0) {
                continue;
            }
            File file2 = new File(path);
            File[] imageFiles = file2.listFiles();
            if (isEmptyArrays(imageFiles)) {
                continue;
            }
            for (File file3 : imageFiles) {
                // 检索到的文件
                logger.info("检索到的文件{}", file3.getName());
                result.add(path + separator + file3.getName());
            }
            logger.info("目录\"{}\"下有{}子目录", path, imageFiles.length);
        }
        return result;
    }

    /**
     * 对所有待删除的图片重命名
     *
     * @param picturesInMarkdown 在md中使用的图片集合
     * @param allPictures        搜索到的所有的图片列表
     */
    private static void doReName(Set<String> picturesInMarkdown, Set<String> allPictures) {
        Set<String> newPicInMd = picturesInMarkdown.stream()
                .map(ele -> ele.replaceAll("/", Matcher.quoteReplacement(separator)))
                .collect(Collectors.toSet());
        allPictures.stream()
                .filter(img -> newPicInMd.stream().noneMatch(img::contains))
                .forEach(PictureClear::reName);
    }

    /**
     * 将文件移动到 指定名录,同时名字后面追加待删除 名字
     */
    private static void reName(String pic) {
        //获取需要剪切的文件
        File file = new File(pic);
        File absFile = file.getAbsoluteFile();
        String[] dirLayers = file.getPath().split("[/\\\\]");
        // 子目录的名字
        String s = dirLayers[dirLayers.length - 2];
        File targetFile = new File(figureBedPath + separator + s);
        logger.info("将要重命名文件 : " + file);
        File destImage = new File(toDeletePath + separator + "待删除" + dirLayers[dirLayers.length - 1]);
        boolean flag = file.getAbsoluteFile().renameTo(destImage);
        logger.info("重命名结果：{}", flag);
        // 如果是空的文件夹就直接删除掉
        if (flag && targetFile.length() == 0) {
            logger.info("删除了这个空文件夹: " + targetFile);
            targetFile.delete();
        }
    }

    private static void printFileList(Set<String> set, String fileType) {
        logger.info("\n检索到的{}文件如下：=============", fileType);
        set.forEach(logger::info);
        logger.info("检索到的{}文件, 共{}个图片文件", fileType, set.size());
    }

    /**
     * 判断一个数组是否为空
     *
     * @param arrays 数组
     * @return true: 空数组
     */
    public static boolean isEmptyArrays(Object[] arrays) {
        return arrays == null || arrays.length <= 0;
    }
}
