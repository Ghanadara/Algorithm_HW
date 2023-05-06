import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class contentComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        String o1Str = o1.replaceAll("[0-9]", "");
        String o2Str = o2.replaceAll("[0-9]", "");
        return o1Str.compareToIgnoreCase(o2Str);
    }
}

class contentComparatorThenByNumber implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        String o1Int = o1.replaceAll("[^\\d]", "");
        String o2Int = o2.replaceAll("[^\\d]", "");

        int o1Int_1 = Integer.parseInt(o1Int);
        int o2Int_2 = Integer.parseInt(o2Int);
        return Integer.compare(o1Int_1, o2Int_2);
    }
}

class Pair<T1, T2> {
    private final T1 content;
    private final T2 score;

    public Pair(T1 content, T2 score) {
        this.content = content;
        this.score = score;
    }

    public T1 getContent() {
        return content;
    }

    public T2 getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "(" + content + "=" + String.format("%.3f", score) + ")";
    }

}

public class HW1 {
    public static HashMap<Integer, TreeMap<String, Double>> FileToString(String path) {
        HashMap<Integer, TreeMap<String, Double>> response = null;
        contentComparator compStr = new contentComparator();
        Comparator<String> compStrThenNum = compStr.thenComparing(new contentComparatorThenByNumber());
        try {
            try (FileReader rw = new FileReader(path); BufferedReader br = new BufferedReader(rw)) {
                String readLine = null;
                readLine = br.readLine();
                response = new HashMap<>(Integer.parseInt(readLine));
                while ((readLine = br.readLine()) != null) {
                    readLine = readLine.replace("\n", "");
                    String[] splited = readLine.split(" ");
                    int userId = Integer.parseInt(splited[0]);
                    String content = splited[1];
                    double score = Double.parseDouble(splited[2]);

                    if (!response.containsKey(userId)) {
                        response.put(userId, new TreeMap<>(compStrThenNum));
                    }
                    TreeMap<String, Double> contentsScore = response.get(userId);
                    contentsScore.put(content, score);
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
        return response;
    }

    public static void NormalizationScore(TreeMap<String, Double> normalMap) {
        double total = 0.0;
        for (String key : normalMap.keySet()) {
            total += normalMap.get(key);
        }
        double avg = total / normalMap.size();

        for (String key : normalMap.keySet()) {
            normalMap.replace(key, normalMap.get(key) - avg);
        }
    }

    public static TreeMap<Integer, Double> CalSimilarity(HashMap<Integer, TreeMap<String, Double>> map, int userName) {
        TreeMap<Integer, Double> similarityMap = new TreeMap<>();
        Map<String, Double> user1 = map.get(userName);
        for (int i = 0; i < map.size(); i++) {
            if (i == userName) {
                continue;
            }
            Map<String, Double> user2 = map.get(i);
            Set<String> common = new HashSet<>(user1.keySet());
            common.retainAll(user2.keySet());
            double similarityMother = 0;
            double tempMother = 0;
            double similaritySon = 0;
            for (String key : user1.keySet()) {
                double temp = user1.get(key);
                tempMother += Math.pow(temp, 2);
            }
            for (String key : user2.keySet()) {
                double temp = user2.get(key);
                similarityMother += Math.pow(temp, 2);
            }
            similarityMother = Math.sqrt(similarityMother) * Math.sqrt(tempMother);
            for (String key : common) {
                double temp1 = user1.get(key);
                double temp2 = user2.get(key);
                similaritySon += temp1 * temp2;
            }
            if (similarityMother != 0) {
                similarityMap.put(i, similaritySon / similarityMother);
            } else
                similarityMap.put(i, 0.0);
        }
        return similarityMap;
    }

    public static Map<Integer, Double> TopSimilarfromMap(TreeMap<Integer, Double> similarityMap, int userCount) {
        TreeMap<Integer, Double> descSortByValueMap = new TreeMap<>(
                (s1, s2) -> similarityMap.get(s2).compareTo(similarityMap.get(s1)));
        descSortByValueMap.putAll(similarityMap);

        int i = 0;
        int tempKey = 0;
        for (Integer key : descSortByValueMap.keySet()) {
            if (i == userCount) {
                tempKey = key;
                break;
            } else {
                i++;
            }
        }

        return descSortByValueMap.headMap(tempKey);
    }

    public static TreeMap<String, Double> RecommendationContent(HashMap<Integer, TreeMap<String, Double>> map,
                                                                int userName, Map<Integer, Double> topUser) {
        contentComparator compStr = new contentComparator();
        Comparator<String> compStrThenNum = compStr.thenComparing(new contentComparatorThenByNumber());
        TreeMap<String, Double> recommendMap = new TreeMap<>(compStrThenNum);
        TreeMap<String, Double> user1 = map.get(userName);
        Set<String> contentUser1 = user1.keySet();
        for (int i : topUser.keySet()) {
            Set<String> contentUser2 = map.get(i).keySet();
            contentUser2.removeAll(contentUser1);
            for (String key : contentUser2) {

                if (!recommendMap.containsKey(key)) {
                    recommendMap.put(key, topUser.get(i) * map.get(i).get(key));
                } else {
                    recommendMap.replace(key, recommendMap.get(key) + topUser.get(i) * map.get(i).get(key));
                }
            }
        }
        return recommendMap;
    }

    public static Map<String, Double> TopRecommend(TreeMap<String, Double> recommendMap, int itemCount) {
        TreeMap<String, Double> descSortByValueMap = new TreeMap<>((key1, key2) -> {
            double value1 = recommendMap.get(key1);
            double value2 = recommendMap.get(key2);
            String[] split1 = key1.split("(?<=\\D)(?=\\d)");
            String[] split2 = key2.split("(?<=\\D)(?=\\d)");
            int result = Double.compare(value2, value1);
            if (result == 0) {
                result = split1[0].compareTo(split2[0]);
                if (result == 0) {
                    int num1 = Integer.parseInt(split1[1]);
                    int num2 = Integer.parseInt(split2[1]);
                    result = Integer.compare(num1, num2);
                }
            }
            return result;
        });
        descSortByValueMap.putAll(recommendMap);
        int i = 0;
        String tempKey = "";
        for (String key : descSortByValueMap.keySet()) {
            if (i == itemCount) {
                tempKey = key;
                break;
            } else {
                i++;
            }
        }
        return descSortByValueMap.headMap(tempKey);
    }

    public static void printMap(Map<String, Double> map) {
        List<Pair<String, Double>> printList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            Pair<String, Double> pair = new Pair<>(key, value);
            printList.add(pair);
        }
        System.out.println(printList);
    }

    public static void main(String[] args) {
        HashMap<Integer, TreeMap<String, Double>> map;
        System.out.print("파일 이름, Target 사용자, 참고인 수, 추천 항목 수?");
        Scanner scanner = new Scanner(System.in);
        String line = "";
        line = scanner.nextLine();
        String[] splited = line.split(" ");
        String path = splited[0];
        int userName = Integer.parseInt(splited[1]);
        int userCount = Integer.parseInt(splited[2]);
        int itemCount = Integer.parseInt(splited[3]);

        map = FileToString(path);

        for (Integer key : map.keySet()) {
            TreeMap<String, Double> temp = map.get(key);
            NormalizationScore(temp);
            map.replace(key, temp);
        }
        System.out.println("1. 사용자 " + userName + "의 콘텐츠와 정규화 점수 :");
        printMap(map.get(userName));

        Map<Integer, Double> topUser = TopSimilarfromMap(CalSimilarity(map, userName), userCount);
        System.out.println("2. 유사한 사용자 id와 유사도 리스트");
        for (int key : topUser.keySet()) {
            System.out.println("\t사용자 id:\t" + key + ", 유사도: " + String.format("%.6f", topUser.get(key)));
        }

        Map<String, Double> recommendMap = TopRecommend(RecommendationContent(map, userName, topUser), itemCount);
        System.out.println("3. 사용자 " + userName + "에게 추천할 콘텐츠와 추천 점수");
        printMap(recommendMap);
    }
}