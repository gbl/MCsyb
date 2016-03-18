/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.MCsyb;

import java.util.Iterator;

/**
 *
 * @author gbl
 */
public class MaterialCharacterMap implements Iterable<MaterialCharacterMapInstance> {
    private final byte[] codeToCharMap;
    private final int[] charToCodeMap;
    private final int[] histogram;
    private final String assignableCharacters;
    private int    nAssignedCharacters;
    private final int baseoffset=0x5000;                // Some offset into unicode that has 16*256 valid code points
    
    public MaterialCharacterMap() {
         codeToCharMap = new byte [256*16];
         charToCodeMap = new int [256];
         histogram     = new int [256*16];
         assignableCharacters="bcefhijklmnopqrstuvwxyz1234567890";  // agd are taken
         nAssignedCharacters=0;
         init();
    }

    private void init() {
        // The "unused" value in charToCodeMap can't be 0, because 0 is a
        // perfectly valid value for "Air".
        for (int i=0; i<256; i++) 
            charToCodeMap[i]=-1;
        set(0,  0, " ");                    // Air
        set(1,  0, "SGgDdAa");              // Stone, granite, polished, diorite, p, andesite, p
        set(2,  0, "L");                    // L=Lawn since Grass conflicts with Granite
        set(3,  0, "E");                    // Dirt
        set(4,  0, "C");                    // Cobble
        set(7,  0, "B");                    // Bedrock
        set(9,  0, "W");                    // Water
        set(51, 0, "FFFFFFFFFFFFFFFF");     // Fire, various stages of burning
        set(83, 0, "RRRRRRRRRRRRRRRR");     // Reed - sugar cane, various stages till growth
    }

    private void set(int material, int startmeta, String shortcuts) {
        for (int i=0; i<shortcuts.length(); i++) {
            char c=shortcuts.charAt(i);
            int comboVal=combinedMaterialMetaValue(material, startmeta+i);
            directlySet(comboVal, c);
        }
    }

    private void directlySet(int comboVal, char c) {
        assert(c<256);
        byte b=(byte)c;
        codeToCharMap[comboVal]=b;
        charToCodeMap[b]=comboVal;
    }
    
    private int combinedMaterialMetaValue(int material, int meta) {
        assert(material<256);
        assert(meta<16);
        return material*16+meta;
    }
    
    public char get(int material, byte meta) {
        int comboVal=combinedMaterialMetaValue(material, meta);
        byte b=codeToCharMap[comboVal];
        if (b!=0)
            return (char) b;
        return (char)(comboVal+baseoffset);
    }
    
    public void addOccurence(int material, byte meta) {
        int code=combinedMaterialMetaValue(material, meta);
        if (codeToCharMap[code]==0)
            histogram[combinedMaterialMetaValue(material, meta)]++;
    }
    
    void analyzeHistogram() {
        while (nAssignedCharacters < assignableCharacters.length()) {
            int best=-1;
            for (int i=0; i<histogram.length; i++)
                if (histogram[i]>0
                &&  (best==-1 || histogram[i]>histogram[best]))
                    best=i;
            if (best==-1)                   // No more used histogram entries.
                break;
            directlySet(best, assignableCharacters.charAt(nAssignedCharacters++));
            histogram[best]=0;
        }
    }
    
    @Override
    public Iterator iterator() {
        return new MCMapIterator<MaterialCharacterMapInstance>(this);
    }

    private class MCMapIterator<E> implements Iterator<E> {
        private final MaterialCharacterMap map;
        private int position=0;
        
        MCMapIterator(MaterialCharacterMap map) {
            this.map=map;
        }

        @Override
        public boolean hasNext() {
            for (int i=position; i<256; i++)
                if (map.charToCodeMap[i]!=-1)
                    return true;
            return false;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public E next() {
            for (int i=position; i<256; i++)
                if (map.charToCodeMap[i]!=-1) {
                    position=i+1;
                    MaterialCharacterMapInstance m = new MaterialCharacterMapInstance();
                    m.symbol=(char)i;
                    m.code=map.charToCodeMap[i]+baseoffset;
                    return (E) m;
                }
            return null;
        }
    }
}
