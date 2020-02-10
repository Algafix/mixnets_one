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
library(ggrepel)

dataFake6 = read.csv("data/histrLatencyFakeMixNodes6.data", header=T, sep = " ",
                dec=".", stringsAsFactors = T,
                colClasses=c('numeric','factor'))

data_meanFake6 = aggregate(list(value = dataFake6$value), by=list(Protocol = dataFake6$scenario, maxTime = dataFake6$maxTime), FUN=mean)


pFake6 <- ggplot(data_meanFake6, aes(x=maxTime, y=value/3600, group=Protocol)) +
  geom_line(aes(linetype=Protocol))+
  geom_point(aes(shape=Protocol))+ylab("Latència (hores)")+
  xlab("Temps màxim per creat paquets falsos")+
  theme_grey(base_size = 13)+
  theme(legend.position="top")+
  ggtitle("Latència en funció del temps màxim de\n generació de missatges falsos (límit de 6)")+
  scale_x_continuous(name="Temps màxim per creat paquets falsos (segons)", breaks=unique(data_meanFake$maxTime))+
  theme(plot.title = element_text(hjust = 0.5))+
  geom_label_repel(aes(label = round(value/3600,2)),
                   box.padding   = 0.35, 
                   point.padding = 0.5,
                   segment.color = 'grey50')

pFake6

ggsave(pFake6,file="R/latencia_fake_mixnets6.pdf", width = 5, height = 5, device=cairo_pdf)
