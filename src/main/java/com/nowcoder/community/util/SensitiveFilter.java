package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词过滤器
 */
@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //敏感词词典
    private static final String REPLACEMENT="***";

    //根节点
    private TrieNode rootNode=new TrieNode();

    @PostConstruct
    public void init(){
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
             BufferedReader reader=new BufferedReader(new InputStreamReader(is));
        ){
            String keyword;
            while ((keyword=reader.readLine())!=null){
                //添加敏感词到Trie树中
                this.addKeyword(keyword);
                }
        }catch (IOException e){
            logger.error("加载敏感词词典失败", e.getMessage());
        }

    }

    //添加敏感词到Trie树中
    private void addKeyword(String keyword) {
        TrieNode temNode=rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c=keyword.charAt(i);
            TrieNode subNode = temNode.getSubNode(c);
            if (subNode==null) {
                //如果不存在该字符对应的子节点，则创建该子节点
                subNode=new TrieNode();
                temNode.addSubNode(c, subNode);
            }

            //指向字节的，进入下一轮循环
            temNode=subNode;

            //设置结束标志
            if (i==keyword.length()-1) {
                subNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text 带过滤文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }

        //指针1
        TrieNode temNode=rootNode;
        //指针2
        int begin=0;
        //指针3
        int position=0;
        //结果
        StringBuilder sb=new StringBuilder();
        while (position<text.length()){
            char c=text.charAt(position);

            //跳过符号
            if(isSymbol(c)){
                //若指针1处于根节点，则将符号添加到结果中，让指针2向下一步走
                if(temNode==rootNode){
                    sb.append(c);
                    begin++;
                }
                //无论符号在开头还是中间，都将指针3向下一步走
                position++;
                continue;
            }
            temNode=temNode.getSubNode(c);
            if(temNode==null){
                //以begin开始的字符串不在敏感词词典中，将其添加到结果中
                sb.append(text.charAt(begin));
                //进入下一个位置
                position=++begin;
                //重定向指向根节点
                temNode=rootNode;
            }else if(temNode.isKeywordEnd()){
                //找到敏感词，将其替换为***
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin=++position;
                //重定向指向根节点
                temNode=rootNode;
            }else{
                //检查下一个字符
                position++;
            }
        }
        //将剩余的字符添加到结果中
        sb.append(text.substring(begin));
        return sb.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c){
        //0x2e80-0x9fff为中文符号范围
        return CharUtils.isAsciiAlphanumeric(c) && (c<0x2e80 || c>0x9fff);
    }

    //前缀数
    private class TrieNode {

        //关键词的结束标志
        private boolean isKeywordEnd=false;

        //子节点(key是下级字符，value是下级节点)
        private Map<Character, TrieNode> subNodes=new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        //获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }

}
