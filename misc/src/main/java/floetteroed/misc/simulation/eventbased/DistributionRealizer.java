package floetteroed.misc.simulation.eventbased;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class DistributionRealizer {

	public static double drawExponential(final double lambda, final Random rnd) {
		return (-Math.log(rnd.nextDouble()) / lambda);
	}

	public static <E> E drawElement(final List<E> elements,
			final List<Double> weights, final Random rnd) {
		double totalWeightSum = 0;
		for (Double weight : weights) {
			totalWeightSum += weight;
		}
		return drawElement(elements, weights, totalWeightSum, rnd);
	}

	public static <E> E drawElement(final List<E> elements,
			final List<Double> weights, final double totalWeightSum,
			final Random rnd) {
		final double u = totalWeightSum * rnd.nextDouble();
		double weightSum = 0;
		for (int i = 0; i < elements.size(); i++) {
			weightSum += weights.get(i);
			if (weightSum >= u) {
				return elements.get(i);
			}
		}
		return elements.get(elements.size() - 1);
	}

	// TODO NEW
	public static <E> E drawElement(final Map<E, Double> elements2weights,
			final Random rnd) {
		final List<E> elements = new ArrayList<E>(elements2weights.size());
		final List<Double> weights = new ArrayList<Double>(
				elements2weights.size());
		double weightSum = 0;
		for (Map.Entry<E, Double> transition : elements2weights.entrySet()) {
			elements.add(transition.getKey());
			weights.add(transition.getValue());
			weightSum += transition.getValue();
		}
		return drawElement(elements, weights, weightSum, rnd);
	}

}
