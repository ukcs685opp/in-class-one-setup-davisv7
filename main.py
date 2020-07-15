from random import random, choice, randint, choices, uniform
from makesettings import settings_text
from os import popen, getcwd, path
from math import sqrt, ceil, floor
from copy import deepcopy


class Individual:
    def __init__(self, weights, mrate, generations):
        self.weights = weights
        self.mrate = mrate
        self.T = generations
        self.ub = 1
        self.lb = 0

    def get_fitness(self):
        pass
        # this would be where the sim is run individually, but instead the report is loaded to determine fitness.
        # this is because the sim can be ran in batch mode
        # use weights to determine which report relates to this set of weights

    def mutate(self, t):
        chance = random()
        if chance < self.mrate:
            pass
        else:
            for i in range(len(self.weights)):
                digit = randint(0, 1)
                if digit:
                    self.weights[i] = round(self.mutation_function(t, self.weights[i] - self.lb), 2)
                else:
                    self.weights[i] += round(self.mutation_function(t, self.ub - self.weights[i]), 2)

    # non uniform mutation which takes as a parameter, the current generation

    def __repr__(self):
        return str(self.weights)

    def mutation_function(self, t, y):
        r = random()
        return y * (1 - r ** (1 - (t / self.T) ** 5))


def weighted_random_choice(choices):
    # https://stackoverflow.com/questions/10324015/fitness-proportionate-selection-roulette-wheel-selection-in-python
    max = sum(choices.values())
    pick = random.uniform(0, max)
    current = 0
    for key, value in choices.items():
        current += value
        if current > pick:
            return key


class Sim:
    def __init__(self, popsize, generations, mrate):
        self.popsize = popsize
        self.generations = generations
        self.gen_num = 0
        self.mrate = mrate
        self.alpha = 0.25  # used in crossover, alpha is some value between 0 and 1
        self.dimension = 3
        self.population = []
        self.fitnesses = {}
        self.best_individual = None
        self.settings_filename = "ga_settings.txt"
        self.reports_loc = "reports"

    def run(self):
        self.populate()
        for i in range(self.generations):
            self.gen_num = i
            self.repopulate()

    def populate(self):
        for i in range(self.popsize):
            weights = [round(random(), 2) for x in range(self.dimension)]
            self.population.append(Individual(weights, self.mrate, self.generations))

    def repopulate(self):
        self.get_fitnesses()
        self.print_best_individual()
        self.select()
        self.mutate_all()

    def get_fitnesses(self):
        # THIS is where the simulation is run in batch mode

        # create settings.txt
        weights = "[" + ";".join([str(x.weights)[1:-1] for x in self.population]) + ";]"

        # print(settings_text.format(weights))
        # save settings
        with open(self.settings_filename, "w") as fileobj:
            fileobj.write(settings_text.format(weights))

        print("Round {}: Initiated".format(self.gen_num))

        # run sim in batch
        command = '"C:\\Program Files\\Java\\jdk-13.0.2\\bin\\java.exe" "-javaagent:C:\\Program Files\\JetBrains\\IntelliJ IDEA 2020.1.3\\lib\\idea_rt.jar=63024:C:\\Program Files\\JetBrains\\IntelliJ IDEA 2020.1.3\\bin" -Dfile.encoding=UTF-8 -classpath "D:\\Box Sync\\Box Sync\\Research\\One-Simulator\\bin;D:\\Box Sync\\Box Sync\\Research\\One-Simulator\\lib\\DTNConsoleConnection.jar;D:\\Box Sync\\Box Sync\\Research\\One-Simulator\\lib\\ECLA.jar;C:\\Users\\v1ntage\\.m2\\repository\\junit\\junit\\4.11\\junit-4.11.jar;C:\\Users\\v1ntage\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar" core.DTNSim -b {} {}'.format(
            self.popsize, self.settings_filename)

        stream = popen(command)
        output = stream.read()  # need this or the program will not wait until the end of the sim
        # print(output)

        print("Round {}: Completed".format(self.gen_num))

        # get fitness for each individual
        for i in range(self.popsize):
            weight_set = str(self.population[i].weights)[1:-1]
            filename = "HyperCubeRouter_{}_MessageStatsReport.txt".format(weight_set)
            fitness_vector = []
            with open(path.join(getcwd(), self.reports_loc, filename), "r") as fileobj:
                lines = [x.strip("\n") for x in fileobj.readlines()]
                # average delivery delay (latency_avg) 12
                avg_delivery_delay = float(lines[12].split()[1])
                # inverse delivery probability (delivery_prob) 9
                inv_delivery_prob = 1.0 / float(lines[9].split()[1])
                # overhead ratio (overhead_ratio) 11
                overhead_ratio = float(lines[11].split()[1])
                fitness_vector = [avg_delivery_delay, inv_delivery_prob, overhead_ratio]
            fitness = sqrt(sum(map(lambda x: x * x, fitness_vector)))
            self.fitnesses[self.population[i]] = fitness

    def select(self):
        # selection in four stages
        potentials = []
        new_population = []
        # new random
        for i in range(floor(self.popsize / 4.0)):
            weights = [round(random(), 2) for _ in range(self.dimension)]
            potentials.append(Individual(weights, self.mrate, self.generations))

        # elitism
        elites = sorted(self.population, key=lambda x: self.fitnesses[x])[:ceil(self.popsize / 4.0)]
        [potentials.append(deepcopy(x)) for x in elites]

        # fitness proportionate (roulette)
        for i in range(floor(self.popsize / 4.0)):
            potentials.append(deepcopy(weighted_random_choice(self.fitnesses)))

        # old random (selected at random from old population)
        for i in range(floor(self.popsize / 4.0)):
            potentials.append(deepcopy(choice(self.population)))

        for i in range(self.popsize):
            l, r = choices(potentials, k=2)
            new_population.append(self.crossover(l, r))
        self.population = new_population

    def mutate_all(self):
        for i in range(self.popsize):
            self.population[i].mutate(self.gen_num)

    def crossover(self, l, r):
        new_weights = []
        for p1, p2 in zip(l.weights, r.weights):
            ps = sorted([p1, p2])
            ps[0] = max(ps[0] * self.alpha, 0)
            ps[1] = min(1, ps[1] * (1 + self.alpha))
            p3 = round(uniform(ps[0], ps[1]), 2)
            new_weights.append(p3)
        return Individual(new_weights, self.mrate, self.generations)

    def print_best_individual(self):
        best_guy = max(self.population, key=lambda k: self.fitnesses[k])
        filename = "HyperCubeRouter_{}_MessageStatsReport.txt".format(str(best_guy.weights)[1:-1])
        fitness_vector = []
        with open(path.join(getcwd(), self.reports_loc, filename), "r") as fileobj:
            lines = [x.strip("\n") for x in fileobj.readlines()]
            # average delivery delay (latency_avg) 12
            avg_delivery_delay = float(lines[12].split()[1])
            # inverse delivery probability (delivery_prob) 9
            delivery_prob = float(lines[9].split()[1])
            # overhead ratio (overhead_ratio) 11
            overhead_ratio = float(lines[11].split()[1])
            fitness_vector = [avg_delivery_delay, delivery_prob, overhead_ratio]
        print("""Best fitness: 
    Weights:        {}
    Average Delay:  {}
    Delivery Prob:  {}
    Overhead Ratio: {}""".format(best_guy.weights, avg_delivery_delay, delivery_prob, overhead_ratio))


def main():
    sim = Sim(popsize=2, generations=1000, mrate=0.01)
    sim.run()


if __name__ == '__main__':
    main()
