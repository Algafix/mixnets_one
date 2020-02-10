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
library(TeachingDemos)
library(ggrepel)

dataLatencyLong = read.csv("data/histrLatencyMixNodesLong.data", header=T, sep = " ",
                dec=".", stringsAsFactors = T,
                colClasses=c('numeric','factor'))

data_meanLatencyLong = aggregate(list(value = dataLatencyLong$value), 
                             by=list(mix = dataLatencyLong$mix, Protocol = dataLatencyLong$scenario), FUN=mean)

pLatencyLong <- ggplot(data_meanLatencyLong, aes(x=mix, y=value/3600, group=Protocol)) +
  geom_line(aes(linetype=Protocol))+
  geom_point(aes(shape=Protocol))+ylab("Latència (hores)")+
  xlab("Nodes Mix")+
  theme_grey(base_size = 13)+
  theme(legend.position="top")+
  ggtitle("Latència en funció del número de nodes mix\n (140000s)")+
  scale_x_continuous(name="Nodes Mix", breaks=unique(data_meanLatencyLong$mix))+
  theme(plot.title = element_text(hjust = 0.5))+
  geom_label_repel(aes(label = round(value/3600,2)),
                   box.padding   = 0.35, 
                   point.padding = 0.5,
                   segment.color = 'grey50')
  

pLatencyLong

ggsave(pLatencyLong,file="R/latencia_mixnets_long.pdf", width = 5, height = 5, device=cairo_pdf)
