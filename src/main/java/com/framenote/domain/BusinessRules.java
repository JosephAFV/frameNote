package com.framenote.domain;
/** Reglas puras de dominio, independientes de HTTP y de la base de datos. */
public final class BusinessRules {
  private BusinessRules() {}
  public static void validTimestamp(long ms, long durationMs) {
    if(durationMs<=0 || ms<0 || ms>=durationMs) throw new IllegalArgumentException("Instante fuera de la duración del video");
  }
  public static void validQuota(long used, long reserved, long incoming, long max) {
    if(used<0 || reserved<0 || incoming<=0 || max<0 || used>max || reserved>max-used || incoming>max-used-reserved)
      throw new IllegalArgumentException("Cuota de almacenamiento excedida");
  }
  public static void validCoordinates(Double x,Double y) {
    if((x==null)!=(y==null)) throw new IllegalArgumentException("Ambas coordenadas deben existir");
    if(x!=null && (!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)) throw new IllegalArgumentException("Coordenadas fuera de 0..1");
  }
  public static void requireEditable(String state) {
    if("APPROVED".equals(state)||"ARCHIVED".equals(state)) throw new IllegalArgumentException("Proyecto no editable");
  }
  public static void validFeedbackTransition(String current,String next) {
    boolean good = switch(current) {case "PENDING" -> "IN_PROGRESS".equals(next)||"RESOLVED".equals(next);
      case "IN_PROGRESS" -> "RESOLVED".equals(next); case "RESOLVED" -> "REOPENED".equals(next);
      case "REOPENED" -> "IN_PROGRESS".equals(next)||"RESOLVED".equals(next); default -> false;};
    if(!good) throw new IllegalArgumentException("Transición de observación inválida");
  }
}
