package work.ready.examples.distributed_computing;

import org.apache.ignite.lang.IgniteClosure;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.service.BusinessService;
import work.ready.core.tools.StopWatch;

import java.util.*;

public class ComputeService extends BusinessService {

    private static List<Double> sampleData;

    public String displayWords(String text) {
        var computer = Cloud.compute();
        for (String word : text.split(" ")) {
            computer.run(() -> System.err.println(word));
        }
        return "please see the result in the console of your nodes.";
    }

    public int countWords(String text) {
        var computer = Cloud.compute();

        // Execute closure on all cluster nodes.
        Collection<Integer> res = computer.apply(String::length, Arrays.asList(text.split(" ")));

        // Add all the word lengths received from cluster nodes.
        int total = res.stream().mapToInt(Integer::intValue).sum();
        return total;
    }

    public int letterCountTask(String text) {
        var computer = Cloud.compute();
        // Execute the task on the cluster and wait for its completion.
        return computer.execute(LetterCountComputeTask.class, text);
    }

    public double singletonCompute(List<Double> list){
        var computer = Cloud.compute();
        Double res = computer.apply((IgniteClosure<List<Double>, Double>) this::sum, list);
        return res;
    }

    private static List<Double> generateData() {
        List<Double> bigData = new ArrayList<>();
        int total = 10000 * 100;
        int i = 0;
        while (i < total) {
            bigData.add(Math.random());
            i ++;
        }
        return bigData;
    }

    public List<Double> getSampleData() {
        if(sampleData == null) sampleData = generateData();
        return sampleData;
    }

    public double standardDeviation(List<Double> list){
        var computer = Cloud.compute();
        List<List<Double>> groupList = new ArrayList<>();
        int slice = 10;
        int total = list.size();
        var counter = new StopWatch();
        if(total > slice && slice > 1) {
            int each = Double.valueOf(Math.floor(total / slice)).intValue();
            int currentIndex = 0;
            while (true) {
                if(currentIndex + each >= total) {
                    groupList.add(new ArrayList<>(list.subList(currentIndex, total)));
                    break;
                } else {
                    groupList.add(new ArrayList<>(list.subList(currentIndex, currentIndex + each)));
                }
                currentIndex += each;
            }
        } else {
            groupList.add(list);
        }
        System.out.println("grouping cost: " + counter.elapsedSeconds());
        counter.reset();
        System.out.println("computing mean: start");
        Collection<Double> meanRes = computer.apply((IgniteClosure<List<Double>, Double>) this::sum, groupList);
        double mean = meanRes.stream().mapToDouble(Double::doubleValue).sum()/total;
        System.out.println("mean: " + mean + ", cost: " + counter.elapsedSeconds());
        System.out.println("computing ssd: start");
        Collection<Double> ssdRes = computer.apply((IgniteClosure<List<Double>, Double>) data->ssd(data, mean), groupList);
        double ssd = ssdRes.stream().mapToDouble(Double::doubleValue).sum();
        System.out.println("ssd: " + ssd + ", cost: " + counter.elapsedSeconds());
        double variance = ssd/(total - 1); // sample variance
        System.out.println("Sample Variance: " + variance);
        double std = Math.sqrt(variance);
        System.out.println("Standard Deviation: " + std);
        return std;
    }

    public double sum(List<Double> list) {
        var counter = new StopWatch();
        double sum = list.stream().mapToDouble(Double::doubleValue).sum();
        System.out.println("Sum size: " + list.size() + " => " + sum + ", cost " + counter.elapsedSeconds());
        return sum;
    }

    // compute sum of squared differences
    public double ssd(List<Double> list, double mean) {
        double ssd = list.stream().mapToDouble(each->{
            double diff = each - mean;
            return diff * diff;
        }).sum();
        return ssd;
    }

    public double standardDeviationLocal(List<Double> list) {
        var counter = new StopWatch();
        double mean = mean(list);
        double df = list.size() - 1;
        double minusSum = 0;
        int size = list.size();
        String toPrint = "";
        for (int i = 0; i < size; i++) {
            double num = list.get(i);
            if(size < 100) {
                toPrint += "(" + num + " - " + mean + ")^2  +  ";
            }
            double diff = num - mean;
            minusSum += diff * diff;
        }
        if(size < 100) {
            toPrint = toPrint.substring(0, toPrint.length() - 3);
            System.out.println(toPrint);
        }
        double variance = minusSum / df;
        System.out.println("Variance: " + minusSum + " * (1 / " + (size - 1) + ")  =  " + variance);
        double sd = Math.sqrt(variance);
        System.out.println("Standard Deviation: root(" + variance + ")  =  " + sd + ", cost: " + counter.elapsedSeconds());
        return sd;
    }

    public double mean(List<Double> list) {
        double sum = 0;
        if(list.size() < 100) {
            System.out.print("Numbers: ");
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            double num = list.get(i);
            if(list.size() < 100) {
                System.out.print(num + " ");
            }
            sum += num;
        }
        System.out.println();
        double mean = sum / size;
        System.out.println("Mean: " + mean);
        return mean;
    }
}
