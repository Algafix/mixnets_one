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
library(plyr)

dataOverheadFake6 = read.csv("data/histrOverheadFakeMixNodes6.data", header=T, sep = " ",
                dec=".", stringsAsFactors = T,
                colClasses=c('numeric','factor','factor'))

epiOverheadFake6 = subset(dataOverheadFake6,
                          protocol == "MixnetEpidemicRouter")
epiOverheadFake6$protocol <- NULL
epiOverheadFake6 <- ddply(epiOverheadFake6, "temps", transform, label_ypos=cumsum(quantitat))

pEOverheadFake6 <- ggplot(data=epiOverheadFake6, aes(x=temps, y=quantitat, fill=tipus)) +
  labs(fill = "Tipus:")+
  geom_bar(stat="identity")+
  theme_grey(base_size = 13)+
  theme(legend.position="top")+
  ggtitle("Latència en funció del temps màxim de\n generació de missatges falsos (límit de 6)\nEpidemic Routing")+
  scale_x_continuous(name="Temps màxim per creat paquets falsos (segons)", breaks=unique(epiOverheadFake6$temps))+
  scale_y_continuous(name="Quantitat de missatges")+
  theme(plot.title = element_text(hjust = 0.5))+
  geom_text(aes(label=quantitat, y=label_ypos), vjust=1.6, size=3.5)

pEOverheadFake6
ggsave(pEOverheadFake6,file="R/overhead_epidemic_fake_mixnets6.pdf", width = 5, height = 5, device=cairo_pdf)

snwOverheadFake6 = subset(dataOverheadFake6,
                          protocol == "MixnetSnWRouter")
snwOverheadFake6$protocol <- NULL
snwOverheadFake6 <- ddply(snwOverheadFake6, "temps", transform, label_ypos=cumsum(quantitat))

pSnWOverheadFake6 <- ggplot(data=snwOverheadFake6, aes(x=temps, y=quantitat, fill=tipus)) +
  labs(fill = "Tipus:")+
  geom_bar(stat="identity")+
  theme_grey(base_size = 13)+
  theme(legend.position="top")+
  ggtitle("Latència en funció del temps màxim de\n generació de missatges falsos (límit de 6)\nSpray and Wait")+
  scale_x_continuous(name="Temps màxim per creat paquets falsos (segons)", breaks=unique(epiOverheadFake6$temps))+
  scale_y_continuous(name="Quantitat de missatges")+
  theme(plot.title = element_text(hjust = 0.5))+
  geom_text(aes(label=quantitat, y=label_ypos), vjust=1.6, size=3.5)

pSnWOverheadFake6
ggsave(pSnWOverheadFake6,file="R/overhead_snw_fake_mixnets6.pdf", width = 5, height = 5, device=cairo_pdf)

