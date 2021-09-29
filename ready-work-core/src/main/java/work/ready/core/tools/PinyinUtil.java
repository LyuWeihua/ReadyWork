package work.ready.core.tools;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;

import java.io.File;
import java.util.*;

public class PinyinUtil {
	private static final Log logger = LogFactory.getLog(PinyinUtil.class);

	public static String getPinyin(String zhStr) {
		StringBuilder pinyin = new StringBuilder();
		char[] zhCharArray = zhStr.toCharArray();
		for (char zhChar : zhCharArray) {
			pinyin.append(getPinyin(zhChar));
		}
		return pinyin.toString();
	}

	public static String getPinyin(char zhCh) {
		String zhChStr = Character.toString(zhCh);
		if (StrUtil.isChinese(zhCh)) {
			try {
				return PinyinHelper.toHanyuPinyinStringArray(zhCh, format)[0];
			} catch (Exception e) {
			}
		}
		return zhChStr;
	}

	public static String getPinyinHeader(String zhStr) {
		StringBuilder pinyin = new StringBuilder();
		char[] zhCharArray = zhStr.toCharArray();
		for (char zhChar : zhCharArray) {
			String str = getPinyin(zhChar);
			if (str.length() > 0) {
				pinyin.append(str.charAt(0));
			}
		}
		return pinyin.toString();
	}

	public static String[] getPinyin(char ch, HanyuPinyinToneType toneType, HanyuPinyinVCharType vcharType) {
		try {

			if(StrUtil.isChinese(ch)) {
				HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
				format.setToneType(toneType!=null ? toneType : HanyuPinyinToneType.WITHOUT_TONE);
				format.setVCharType(vcharType!=null ? vcharType : HanyuPinyinVCharType.WITH_V);
				String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch, format);
				if(pinyin!=null && pinyin.length>0) {
					return pinyin;
				}
			}
		} catch (BadHanyuPinyinOutputFormatCombination e) {}
		return new String[] {String.valueOf(ch)};
	}

	public static String[] getPinyin(char ch, int caseType, int toneType, int vcharType) {
		int two = 2;
		if(toneType!=1 && toneType!=two) {
			vcharType=0;
		}
		String[] pinyin = getPinyin(ch, toneType==1?HanyuPinyinToneType.WITHOUT_TONE:(toneType==two?HanyuPinyinToneType.WITH_TONE_NUMBER:HanyuPinyinToneType.WITH_TONE_MARK), vcharType==1?HanyuPinyinVCharType.WITH_V:(vcharType==two?HanyuPinyinVCharType.WITH_U_AND_COLON:HanyuPinyinVCharType.WITH_U_UNICODE));
		if(caseType==1 || caseType==two) {
			for(int i=0; i<pinyin.length; i++) {
				if(caseType==1) {
					pinyin[i] = pinyin[i];
				} else {
					pinyin[i] = pinyin[i].toUpperCase();
				}
			}
		}
		return pinyin;
	}

	public static String getPinyin(String sentence, int index, HanyuPinyinToneType toneType, HanyuPinyinVCharType vcharType) {
		String[] pinyin = getPinyin(sentence.charAt(index), toneType, vcharType);
		
		if(pinyin.length>1) {
			boolean b = ((toneType==null||toneType==HanyuPinyinToneType.WITHOUT_TONE)&&(vcharType==null||vcharType==HanyuPinyinVCharType.WITH_V));
			String[] pinyins = b ? pinyin : getPinyin(sentence.charAt(index), HanyuPinyinToneType.WITHOUT_TONE, HanyuPinyinVCharType.WITH_V);
			List<String> words = new ArrayList<>();
			int left = Math.max(index - duoyinziMax + 1, 0), len = sentence.length();
			for(int i=left; i<=index; i++) {
				for(int j=Math.max(index, i+1); j<Math.min(i+duoyinziMax,len); j++) {
					words.add(sentence.substring(i, j+1));
				}
			}
			
			words.add(sentence.substring(index, index+1));
			for(String word : words) {
				String py = duoyinzi.get(word);
				if(py!=null) {
					for(int i = 0; i < pinyins.length; i++){
						if(py.equals(pinyins[i])){
							return pinyin[i];
						}
					}
				}
			}
		}
		return pinyin[0];
	}

	public static String[] getPinyin(String sentence, int caseType, int toneType, int vcharType) {
		int two = 2;
		if(toneType!=1 && toneType!=two)
		 {
			
			vcharType=0;
		}
		String[] pinyin = new String[sentence.length()];
		for(int i=0,len=pinyin.length; i<len; i++) {
			String py = getPinyin(sentence, i, toneType==1?HanyuPinyinToneType.WITHOUT_TONE:(toneType==two?HanyuPinyinToneType.WITH_TONE_NUMBER:HanyuPinyinToneType.WITH_TONE_MARK)
					, vcharType==1?HanyuPinyinVCharType.WITH_V:(vcharType==two?HanyuPinyinVCharType.WITH_U_AND_COLON:HanyuPinyinVCharType.WITH_U_UNICODE));
			if(caseType==1) {
				pinyin[i] = StrUtil.firstCharToUpperCase(py);
			} else if(caseType==two) {
				pinyin[i] = py.toUpperCase();
			} else {
				pinyin[i] = py;
			}
		}
		return pinyin;
	}

	public static final Comparator<String> ZH_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if (o1 == null) {
				if (o2 == null) {
					
					return 0;
				}
				else {
					
					return -1;
				}
			} else {
				if (o2 == null) {
					
					return 1;
				} else {
					
					int min = Math.min(o1.length(), o2.length());
					for (int i = 0; i < min; i++) {
						char c1 = o1.charAt(i), c2 = o2.charAt(i);
						if (StrUtil.isChinese(c1)) {
							if (!StrUtil.isChinese(c2)) {
								
								return 1;
							} else if (c1 != c2)
							 {
								
								return getPinyin(c1).compareTo(getPinyin(c2));
							}
						} else {
							if (StrUtil.isChinese(c2)) {
								
								return -1;
							} else if (c1 != c2)
							 {
								
								return String.valueOf(c1).compareToIgnoreCase(String.valueOf(false));
							}
						}
					}
					
					return o1.length() < min ? -1 : (o2.length() < min ? 1 : 0);
				}
			}
		}
	};

	public static <Entity> Comparator<Entity> zhStringFieldComparator(Class<Entity> clazz, final String zhStringField){
		return new Comparator<Entity>() {
			@Override
			public int compare(Entity o1, Entity o2) {
				try{
					String left = (String)ClassUtil.getField(ClassUtil.findField(o1.getClass(), zhStringField), o1);
					String right = (String)ClassUtil.getField(ClassUtil.findField(o2.getClass(), zhStringField), o2);
					return ZH_COMPARATOR.compare(left, right);
				}catch(Exception e) {
					e.printStackTrace();
				}
				return 0;
			}
		};
	}

	public static class PinyinFileNameComparator implements Comparator<File> {
		@Override
		public int compare(File o1, File o2) {
			String left = FileUtil.getFileName(o1);
			String right = FileUtil.getFileName(o1);
			return ZH_COMPARATOR.compare(left, right);
		}
	}

	private static HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
	private static Map<String, String> duoyinzi = new HashMap<>();
	private static int duoyinziMax = 1;
	static {
		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		format.setVCharType(HanyuPinyinVCharType.WITH_V);

		try {
			FileUtil.TextReader reader = new FileUtil.TextReader(FileUtil.getStream("duoyinzi.txt"), Constant.DEFAULT_ENCODING);
			String line = null;
			while(StrUtil.notBlank(line=reader.read())) {
				String[] split = line.split("#");
				String[] words = split[1].split("\\s+");
				for(String word:words) {
					duoyinzi.put(word, split[0]);
					if(word.length()>duoyinziMax) {
						duoyinziMax = word.length();
					}
				}
			}
			reader.close();
			logger.info("duoyinzi words: " + duoyinzi.size() + ", max length: " + duoyinziMax);
		} catch (Exception e) {
			logger.warn("fail to load duoyinzi.txt: " + e.getMessage());
		}
	}
}
