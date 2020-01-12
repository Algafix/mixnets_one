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
tere4Palette <- c("#4B574D", "#609F80", "#AF420A", "#CBB345")
tere6Palette<-c("#004C3F", "#7DB343", "#E64A19","#398E3D", "#26A59A", "#FEC107")
tere9Palette<-c("#004C3F", "#7DB343", "#E64A19","#398E3D", "#26A59A", "#FEC107","#ef8a62","#67a9cf","#91cf60")

data = read.csv("data/histr.data", header=T, sep = " ",
                dec=".", stringsAsFactors = T,
                colClasses=c('numeric','factor'))

p5 <- ggplot(data, aes(x=scenario, y=value*3/3600)) +
  #scale_x_discrete(labels=colorder)+
  geom_boxplot(outlier.shape = NA, aes(colour = scenario)) +
  geom_jitter(alpha=0.4, colour="gray", width = 0.4, size=0.2)  +
  scale_color_manual(values=tere6Palette,name = "Scenario")+
  scale_fill_manual(values=tere6Palette)+
  #scale_y_continuous(trans='log10',limits=c(0.01, 1000),labels = comma) +
  #scale_y_continuous(breaks=(0.1,10,100,1000), labels = scales::comma,trans='log10',limits=c(0.01,900))+
  theme_bw()+#theme(panel.grid.major = element_blank(),panel.grid.minor = element_blank())+
  scale_y_continuous(labels = scales::comma, trans='log10', breaks=c(0.1,1, 10, 100) )+
  ylab("Latency (hours)")+
  xlab("Scenario")+
  ggtitle("Latency Time") +
  theme_bw()+#theme(panel.grid.major = element_blank(),panel.grid.minor = element_blank())+
  theme(plot.title = element_text(family = "Trebuchet MS", color="#696969",  size=12, hjust=0.5)) +
  theme(axis.title = element_text(family = "Trebuchet MS", color="#696969",  size=12, hjust=0.5)) +
  theme(axis.text = element_text(family = "Trebuchet MS", color="#696969",  size=8, hjust=0.5)) +
  theme(legend.title = element_text(family = "Trebuchet MS", color="#696969",  size=10, hjust=0.5)) +
  theme(legend.text = element_text(family = "Trebuchet MS", color="#696969",  size=8, hjust=0.5)) +
  #theme(axis.title.x=element_blank())+
  guides(fill = guide_legend(nrow = 2))
p5
ggsave(p5,file="pdf/latency.pdf", width = 5, height = 2,device=cairo_pdf)


