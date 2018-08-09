# Gunnar's java repository

Copyright 2009-2018 Gunnar Flötteröd

All code in this repository is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

The repository contains the following programs:
* **bioroute** for sampling paths on a network
* **cadyts** for conditioning a transport simulator on traffic counts
* **opdyts** for approximately solving simulation-based transportation optimization problems

In addition, **utilities** is used by all of the above.

## BIOROUTE

BIOROUTE samples paths from a network according to arbitrary (not necessarily link-additive) path weights. It can be used as a pre-processor of BIOGEME for the estimation of route choice models, but it can also be used standalone for any other application that requires to sample paths.

Related publications:
* G. Flötteröd and M. Bierlaire. Metropolis-Hastings sampling of paths. Transportation Research Part B, 48:53-66, 2013.

## Cadyts

Cadyts estimates disaggregate demand models of dynamic traffic assignment simulators from traffic counts and vehicle re-identification data.

Cadyts has in the past been used to calibrate the following traffic simulators:
* MATSim (Multi-Agent Transport Simulation Toolkit); integration code can be found at https://github.com/matsim-org/matsim/tree/master/contribs/cadytsIntegration
* SUMO (Simulation of Urban Mobility)
* DRACULA (Dynamic Route Assignment Combining User Learning and Microsimulation)
 
Related publications:
* G. Flötteröd and R. Liu. Disaggregate path flow estimation in an iterated DTA microsimulation. Published online in advance in Journal of Intelligent Transportation Systems, 2013.
* G. Flötteröd, Y. Chen, and K. Nagel. Behavioral calibration and analysis of a large-scale travel microsimulation. Networks and Spatial Economics, 12(4):481-502, 2012.
* G. Flötteröd, M. Bierlaire, and K. Nagel. Bayesian demand calibration for dynamic traffic simulations. Transportation Science, 45(4):541-561, 2011.
 
Related PhD dissertations:
* M. Moyo Oliveros. Calibration of public transit routing for multi-agent simulation. PhD thesis, Technical University of Berlin, Germany, 2013.
* Y. Chen. Adding a comprehensive calibration methodology to an agent-based transportation simulation. PhD thesis, Technical University of Berlin, Germany, 2012.
* G. Flötteröd. Traffic state estimation with multi-agent simulations. PhD thesis, Technical University of Berlin, Germany, 2008.

## Opdyts

Opdyts approximately solves general simulation-based optimization problems subject to transport simulation constraints. 

Integration code with MATSim can be found here: https://github.com/matsim-org/Opdyts-MATSim-Integration

Related journal publications:
* G. Flötteröd. A search acceleration method for optimization problems with transport simulation constraints. Transportation Research Part B, 98:239-260, 2017. Open access.
 
