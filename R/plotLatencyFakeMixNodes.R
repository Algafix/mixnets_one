#Libraries####
#######################################################################################################
library(ggplot2)
library(gridExtra)
library(MASS)
library(survival)
require(utils)
library(fitdistrplus)
library(nlme)
library(TeachingDemos)

dataFake = read.csv("data/histrLatencyFakeMixNodes.data", header=T, sep = " ",
                dec=".", stringsAsFactors = T,
                colClasses=c('numeric','factor'))

data_meanFake = aggregate(list(value = dataFake$value), by=list(Protocol = dataFake$scenario, maxTime = dataFake$maxTime), FUN=mean)

png("R/latencia_fake_mixnets.png")

pFake <- ggplot(data_meanFake, aes(x=maxTime, y=value/3600, group=Protocol)) +
  geom_line(aes(linetype=Protocol))+
  geom_point(aes(shape=Protocol))+ylab("Latència (hores)")+
  xlab("Temps màxim per creat paquets falsos")+
  theme(legend.position="top")+
  ggtitle("Latència en funció del temps màxim de generació de missatges falsos")+
  scale_x_continuous(name="Temps màxim per creat paquets falsos (segons)", breaks=unique(data_meanFake$maxTime))

print(pFake)
dev.off()
pFake
