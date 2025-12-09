# Usamos Payara Micro 5 (JDK 11)
FROM payara/micro:5.2022.5-jdk11

# Copiamos el WAR
COPY ./dist/ROOT.war /opt/payara/deployments/ROOT.war

# Exponemos el puerto
EXPOSE 8080

# Comando de arranque LIMPIO
# --port 8080: Fuerza a Payara a usar este puerto para la web
# --contextRoot /: Hace que tu app cargue en la raíz
CMD ["--deploy", "/opt/payara/deployments/ROOT.war", "--contextRoot", "/", "--port", "8080"]
