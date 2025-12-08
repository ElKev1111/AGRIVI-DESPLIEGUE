package Modelo;

public enum EnumTipo {
    ENTRADA("Entrada"),
    SALIDA("Salida");
    
    private final String valor;
    
    private EnumTipo(String valor) {
        this.valor = valor;
    }
    
    public String getValor() {
        return valor;
    }
    
    public static EnumTipo fromString(String valor) {
        for (EnumTipo tipo : EnumTipo.values()) {
            if (tipo.valor.equals(valor)) {
                return tipo;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return valor;
    }
}