package com.networkanalyzer.model;

public class HttpFrame {

    private int    numero;
    private String srcIp;
    private String dstIp;
    private String srcMac;
    private String dstMac;
    private int    srcPort;
    private int    dstPort;
    private String methode;
    private String url;
    private int    taille;
    private long   timestamp;

    public HttpFrame(int numero,
                     String srcIp,   String dstIp,
                     String srcMac,  String dstMac,
                     int srcPort,    int dstPort,
                     String methode, String url,
                     int taille,     long timestamp) {
        this.numero    = numero;
        this.srcIp     = srcIp;
        this.dstIp     = dstIp;
        this.srcMac    = srcMac;
        this.dstMac    = dstMac;
        this.srcPort   = srcPort;
        this.dstPort   = dstPort;
        this.methode   = methode;
        this.url       = url;
        this.taille    = taille;
        this.timestamp = timestamp;
    }

    public int    getNumero()    { return numero;    }
    public String getSrcIp()     { return srcIp;     }
    public String getDstIp()     { return dstIp;     }
    public String getSrcMac()    { return srcMac;    }
    public String getDstMac()    { return dstMac;    }
    public int    getSrcPort()   { return srcPort;   }
    public int    getDstPort()   { return dstPort;   }
    public String getMethode()   { return methode;   }
    public String getUrl()       { return url;       }
    public int    getTaille()    { return taille;    }
    public long   getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%d] %s:%d → %s:%d  %s %s (%d bytes)",
                numero, srcIp, srcPort, dstIp, dstPort, methode, url, taille);
    }
}
