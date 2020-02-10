
## Anonimitat ##

anonimitat <- function(mixnodes, bundle) (1/bundle)^(mixnodes) * 100

mixnodes <- c(1,3,6);
bundle <- seq(1:10);

percent <- mapply(anonimitat, mixnodes, list(bundle))

res1 <- data.frame("percentatge" = percent[,1],"mixnodes" = mixnodes[1], bundle)
res2 <- data.frame("percentatge" = percent[,2],"mixnodes" = mixnodes[2], bundle)
res3 <- data.frame("percentatge" = percent[,3],"mixnodes" = mixnodes[3], bundle)

resultats <- rbind(res1,res2,res3)

resultats <- transform(resultats, mixnodes = factor(mixnodes))


pAnon <- ggplot(resultats, aes(y=percentatge, x=bundle, group=factor(mixnodes))) +
  geom_line(aes(linetype=mixnodes))+
  geom_point(aes(shape=mixnodes))+
  ylab("Probabilitat de seguir el missatge")+
  xlab("Missatges")+
  theme_grey(base_size = 13)+
  theme(legend.position="top")+
  ggtitle("Mètrica d'anonimitat")+
  scale_x_continuous(name="Mínim de missatges per transferir", breaks=unique(resultats$bundle))+
  theme(plot.title = element_text(hjust = 0.5))


pAnon

ggsave(pAnon,file="R/anonimitat.pdf", width = 5, height = 5, device=cairo_pdf)

