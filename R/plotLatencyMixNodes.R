#Libraries####
#######################################################################################################
library(ggplot2)
library(gridExtra)
library(ggplot2)
library(MASS)
library(survival)
require(utils)
library(fitdistrplus)
library(nlme)
library(MuMIn)
library(TeachingDemos)

dataLatency = read.csv("data/histrLatencyMixNodes.data", header=T, sep = " ",
                dec=".", stringsAsFactors = T,
                colClasses=c('numeric','factor'))

data_meanLatency = aggregate(list(value = dataLatency$value), 
                             by=list(mix = dataLatency$mix, Protocol = dataLatency$scenario), FUN=mean)

#png("R/latencia_mixnets.png")

pLatency <- ggplot(data_meanLatency, aes(x=mix, y=value/3600, group=Protocol)) +
  geom_line(aes(linetype=Protocol))+
  geom_point(aes(shape=Protocol))+ylab("Latència (hores)")+
  xlab("Nodes Mix")+
  theme_grey(base_size = 13)+
  theme(legend.position="top")+
  ggtitle("Latència en funció del número de nodes mix.")+
  scale_x_continuous(name="Nodes Mix", breaks=unique(data_meanLatency$mix))+
  theme(plot.title = element_text(hjust = 0.5))
  

pLatency

ggsave(pLatency,file="R/latencia_mixnets.pdf", width = 5, height = 5, device=cairo_pdf)

#print(pLatency)
#dev.off()
