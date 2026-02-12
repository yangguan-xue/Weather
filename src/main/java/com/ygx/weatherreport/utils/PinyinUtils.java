package com.ygx.weatherreport.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class PinyinUtils {

    /**
     * 判断一个字符是否为中文字符（基于常用汉字Unicode范围）。
     */
    private static boolean isChineseCharacter(char c) {
        // 通过直接比较字符的Unicode码点来判断，这是最清晰且无歧义的方式
        // 范围：\u4E00 - \u9FA5 涵盖了常用的CJK统一表意文字
        return c >= '\u4E00' && c <= '\u9FA5';
    }

    /**
     * 判断字符串是否包含中文字符。
     */
    public static boolean containsChinese(String str) {
        if (!StringUtils.hasText(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (isChineseCharacter(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将包含中文的字符串转换为拼音（全小写，无声调）。
     * 非中文字符（英文、数字、标点）将原样保留。
     *
     * @param chinese 包含中文的字符串
     * @return 转换后的拼音字符串
     */
    public static String toPinyin(String chinese) {
        if (!StringUtils.hasText(chinese)) {
            return chinese;
        }

        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        StringBuilder pinyin = new StringBuilder();
        char[] chars = chinese.trim().toCharArray();

        try {
            for (char c : chars) {
                // 使用新的、无歧义的方法判断中文字符
                if (isChineseCharacter(c)) {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]); // 多音字取第一个读音
                    } else {
                        // 转换失败，保留原字符
                        pinyin.append(c);
                    }
                } else {
                    // 非中文字符，直接追加
                    pinyin.append(c);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            log.error("拼音转换失败", e);
            return chinese; // 转换失败时返回原字符串
        }
        return pinyin.toString();
    }
}