/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package work.ready.core.template.io;

public class FloatingDecimal{
    boolean     isExceptional;
    boolean     isNegative;
    int         decExponent;
    char        digits[];
    int         nDigits;
    int         bigIntExp;
    int         bigIntNBits;
    boolean     mustSetRoundDir = false;
    boolean     fromHex = false;
    int         roundDir = 0;

    static final long   signMask = 0x8000000000000000L;
    static final long   expMask  = 0x7ff0000000000000L;
    static final long   fractMask= ~(signMask|expMask);
    static final int    expShift = 52;
    static final int    expBias  = 1023;
    static final long   fractHOB = ( 1L<<expShift ); 
    static final long   expOne   = ((long)expBias)<<expShift; 
    static final int    maxSmallBinExp = 62;
    static final int    minSmallBinExp = -( 63 / 3 );
    static final int    maxDecimalDigits = 15;
    static final int    maxDecimalExponent = 308;
    static final int    minDecimalExponent = -324;
    static final int    bigDecimalExponent = 324; 

    static final long   highbyte = 0xff00000000000000L;
    static final long   highbit  = 0x8000000000000000L;
    static final long   lowbytes = ~highbyte;

    static final int    singleSignMask =    0x80000000;
    static final int    singleExpMask  =    0x7f800000;
    static final int    singleFractMask =   ~(singleSignMask|singleExpMask);
    static final int    singleExpShift  =   23;
    static final int    singleFractHOB  =   1<<singleExpShift;
    static final int    singleExpBias   =   127;
    static final int    singleMaxDecimalDigits = 7;
    static final int    singleMaxDecimalExponent = 38;
    static final int    singleMinDecimalExponent = -45;

    static final int    intDecimalDigits = 9;

    private static int
    countBits( long v ){

        if ( v == 0L ) return 0;

        while ( ( v & highbyte ) == 0L ){
            v <<= 8;
        }
        while ( v > 0L ) { 
            v <<= 1;
        }

        int n = 0;
        while (( v & lowbytes ) != 0L ){
            v <<= 8;
            n += 8;
        }
        while ( v != 0L ){
            v <<= 1;
            n += 1;
        }
        return n;
    }

    private static FDBigInt b5p[];

    private static synchronized FDBigInt
    big5pow( int p ){
        assert p >= 0 : p; 
        if ( b5p == null ){
            b5p = new FDBigInt[ p+1 ];
        }else if (b5p.length <= p ){
            FDBigInt t[] = new FDBigInt[ p+1 ];
            System.arraycopy( b5p, 0, t, 0, b5p.length );
            b5p = t;
        }
        if ( b5p[p] != null )
            return b5p[p];
        else if ( p < small5pow.length )
            return b5p[p] = new FDBigInt( small5pow[p] );
        else if ( p < long5pow.length )
            return b5p[p] = new FDBigInt( long5pow[p] );
        else {

            int q, r;

            q = p >> 1;
            r = p - q;
            FDBigInt bigq =  b5p[q];
            if ( bigq == null )
                bigq = big5pow ( q );
            if ( r < small5pow.length ){
                return (b5p[p] = bigq.mult( small5pow[r] ) );
            }else{
                FDBigInt bigr = b5p[ r ];
                if ( bigr == null )
                    bigr = big5pow( r );
                return (b5p[p] = bigq.mult( bigr ) );
            }
        }
    }

    private static FDBigInt
    multPow52( FDBigInt v, int p5, int p2 ){
        if ( p5 != 0 ){
            if ( p5 < small5pow.length ){
                v = v.mult( small5pow[p5] );
            } else {
                v = v.mult( big5pow( p5 ) );
            }
        }
        if ( p2 != 0 ){
            v.lshiftMe( p2 );
        }
        return v;
    }

    private static FDBigInt
    constructPow52( int p5, int p2 ){
        FDBigInt v = new FDBigInt( big5pow( p5 ) );
        if ( p2 != 0 ){
            v.lshiftMe( p2 );
        }
        return v;
    }

    private void
    developLongDigits( int decExponent, long lvalue, long insignificant ){
        char digits[];
        int  ndigits;
        int  digitno;
        int  c;

        int i;
        for ( i = 0; insignificant >= 10L; i++ )
            insignificant /= 10L;
        if ( i != 0 ){
            long pow10 = long5pow[i] << i; 
            long residue = lvalue % pow10;
            lvalue /= pow10;
            decExponent += i;
            if ( residue >= (pow10>>1) ){
                
                lvalue++;
            }
        }
        if ( lvalue <= Integer.MAX_VALUE ){
            assert lvalue > 0L : lvalue;

            int  ivalue = (int)lvalue;
            ndigits = 10;
            digits = (char[])(perThreadBuffer.get());
            digitno = ndigits-1;
            c = ivalue%10;
            ivalue /= 10;
            while ( c == 0 ){
                decExponent++;
                c = ivalue%10;
                ivalue /= 10;
            }
            while ( ivalue != 0){
                digits[digitno--] = (char)(c+'0');
                decExponent++;
                c = ivalue%10;
                ivalue /= 10;
            }
            digits[digitno] = (char)(c+'0');
        } else {

            ndigits = 20;
            digits = (char[])(perThreadBuffer.get());
            digitno = ndigits-1;
            c = (int)(lvalue%10L);
            lvalue /= 10L;
            while ( c == 0 ){
                decExponent++;
                c = (int)(lvalue%10L);
                lvalue /= 10L;
            }
            while ( lvalue != 0L ){
                digits[digitno--] = (char)(c+'0');
                decExponent++;
                c = (int)(lvalue%10L);
                lvalue /= 10;
            }
            digits[digitno] = (char)(c+'0');
        }
        char result [];
        ndigits -= digitno;
        result = new char[ ndigits ];
        System.arraycopy( digits, digitno, result, 0, ndigits );
        this.digits = result;
        this.decExponent = decExponent+1;
        this.nDigits = ndigits;
    }

    private void
    roundup(){
        int i;
        int q = digits[ i = (nDigits-1)];
        if ( q == '9' ){
            while ( q == '9' && i > 0 ){
                digits[i] = '0';
                q = digits[--i];
            }
            if ( q == '9' ){
                
                decExponent += 1;
                digits[0] = '1';
                return;
            }
            
        }
        digits[i] = (char)(q+1);
    }

    public FloatingDecimal( double d )
    {
        long    dBits = Double.doubleToLongBits( d );
        long    fractBits;
        int     binExp;
        int     nSignificantBits;

        if ( (dBits&signMask) != 0 ){
            isNegative = true;
            dBits ^= signMask;
        } else {
            isNegative = false;
        }

        binExp = (int)( (dBits&expMask) >> expShift );
        fractBits = dBits&fractMask;
        if ( binExp == (int)(expMask>>expShift) ) {
            isExceptional = true;
            if ( fractBits == 0L ){
                digits =  infinity;
            } else {
                digits = notANumber;
                isNegative = false; 
            }
            nDigits = digits.length;
            return;
        }
        isExceptional = false;

        if ( binExp == 0 ){
            if ( fractBits == 0L ){
                
                decExponent = 0;
                digits = zero;
                nDigits = 1;
                return;
            }
            while ( (fractBits&fractHOB) == 0L ){
                fractBits <<= 1;
                binExp -= 1;
            }
            nSignificantBits = expShift + binExp +1; 
            binExp += 1;
        } else {
            fractBits |= fractHOB;
            nSignificantBits = expShift+1;
        }
        binExp -= expBias;
        
        dtoa( binExp, fractBits, nSignificantBits );
    }

    public FloatingDecimal( float f )
    {
        int     fBits = Float.floatToIntBits( f );
        int     fractBits;
        int     binExp;
        int     nSignificantBits;

        if ( (fBits&singleSignMask) != 0 ){
            isNegative = true;
            fBits ^= singleSignMask;
        } else {
            isNegative = false;
        }

        binExp = (int)( (fBits&singleExpMask) >> singleExpShift );
        fractBits = fBits&singleFractMask;
        if ( binExp == (int)(singleExpMask>>singleExpShift) ) {
            isExceptional = true;
            if ( fractBits == 0L ){
                digits =  infinity;
            } else {
                digits = notANumber;
                isNegative = false; 
            }
            nDigits = digits.length;
            return;
        }
        isExceptional = false;

        if ( binExp == 0 ){
            if ( fractBits == 0 ){
                
                decExponent = 0;
                digits = zero;
                nDigits = 1;
                return;
            }
            while ( (fractBits&singleFractHOB) == 0 ){
                fractBits <<= 1;
                binExp -= 1;
            }
            nSignificantBits = singleExpShift + binExp +1; 
            binExp += 1;
        } else {
            fractBits |= singleFractHOB;
            nSignificantBits = singleExpShift+1;
        }
        binExp -= singleExpBias;
        
        dtoa( binExp, ((long)fractBits)<<(expShift-singleExpShift), nSignificantBits );
    }

    private void
    dtoa( int binExp, long fractBits, int nSignificantBits )
    {
        int     nFractBits; 
        int     nTinyBits;  
        int     decExp;

        nFractBits = countBits( fractBits );
        nTinyBits = Math.max( 0, nFractBits - binExp - 1 );
        if ( binExp <= maxSmallBinExp && binExp >= minSmallBinExp ){

            if ( (nTinyBits < long5pow.length) && ((nFractBits + n5bits[nTinyBits]) < 64 ) ){
                
                long halfULP;
                if ( nTinyBits == 0 ) {
                    if ( binExp > nSignificantBits ){
                        halfULP = 1L << ( binExp-nSignificantBits-1);
                    } else {
                        halfULP = 0L;
                    }
                    if ( binExp >= expShift ){
                        fractBits <<= (binExp-expShift);
                    } else {
                        fractBits >>>= (expShift-binExp) ;
                    }
                    developLongDigits( 0, fractBits, halfULP );
                    return;
                }
                
            }
        }

        double d2 = Double.longBitsToDouble(
            expOne | ( fractBits &~ fractHOB ) );
        decExp = (int)Math.floor(
            (d2-1.5D)*0.289529654D + 0.176091259 + (double)binExp * 0.301029995663981 );
        int B2, B5; 
        int S2, S5; 
        int M2, M5; 
        int Bbits; 
        int tenSbits; 
        FDBigInt Sval, Bval, Mval;

        B5 = Math.max( 0, -decExp );
        B2 = B5 + nTinyBits + binExp;

        S5 = Math.max( 0, decExp );
        S2 = S5 + nTinyBits;

        M5 = B5;
        M2 = B2 - nSignificantBits;

        fractBits >>>= (expShift+1-nFractBits);
        B2 -= nFractBits-1;
        int common2factor = Math.min( B2, S2 );
        B2 -= common2factor;
        S2 -= common2factor;
        M2 -= common2factor;

        if ( nFractBits == 1 )
            M2 -= 1;

        if ( M2 < 0 ){

            B2 -= M2;
            S2 -= M2;
            M2 =  0;
        }
        
        char digits[] = this.digits = new char[18];
        int  ndigit = 0;
        boolean low, high;
        long lowDigitDifference;
        int  q;

        Bbits = nFractBits + B2 + (( B5 < n5bits.length )? n5bits[B5] : ( B5*3 ));
        tenSbits = S2+1 + (( (S5+1) < n5bits.length )? n5bits[(S5+1)] : ( (S5+1)*3 ));
        if ( Bbits < 64 && tenSbits < 64){
            if ( Bbits < 32 && tenSbits < 32){
                
                int b = ((int)fractBits * small5pow[B5] ) << B2;
                int s = small5pow[S5] << S2;
                int m = small5pow[M5] << M2;
                int tens = s * 10;
                
                ndigit = 0;
                q = b / s;
                b = 10 * ( b % s );
                m *= 10;
                low  = (b <  m );
                high = (b+m > tens );
                assert q < 10 : q; 
                if ( (q == 0) && ! high ){
                    
                    decExp--;
                } else {
                    digits[ndigit++] = (char)('0' + q);
                }
                
                if ( decExp < -3 || decExp >= 8 ){
                    high = low = false;
                }
                while( ! low && ! high ){
                    q = b / s;
                    b = 10 * ( b % s );
                    m *= 10;
                    assert q < 10 : q; 
                    if ( m > 0L ){
                        low  = (b <  m );
                        high = (b+m > tens );
                    } else {

                        low = true;
                        high = true;
                    }
                    digits[ndigit++] = (char)('0' + q);
                }
                lowDigitDifference = (b<<1) - tens;
            } else {
                
                long b = (fractBits * long5pow[B5] ) << B2;
                long s = long5pow[S5] << S2;
                long m = long5pow[M5] << M2;
                long tens = s * 10L;
                
                ndigit = 0;
                q = (int) ( b / s );
                b = 10L * ( b % s );
                m *= 10L;
                low  = (b <  m );
                high = (b+m > tens );
                assert q < 10 : q; 
                if ( (q == 0) && ! high ){
                    
                    decExp--;
                } else {
                    digits[ndigit++] = (char)('0' + q);
                }
                
                if ( decExp < -3 || decExp >= 8 ){
                    high = low = false;
                }
                while( ! low && ! high ){
                    q = (int) ( b / s );
                    b = 10 * ( b % s );
                    m *= 10;
                    assert q < 10 : q;  
                    if ( m > 0L ){
                        low  = (b <  m );
                        high = (b+m > tens );
                    } else {

                        low = true;
                        high = true;
                    }
                    digits[ndigit++] = (char)('0' + q);
                }
                lowDigitDifference = (b<<1) - tens;
            }
        } else {
            FDBigInt tenSval;
            int  shiftBias;

            Bval = multPow52( new FDBigInt( fractBits  ), B5, B2 );
            Sval = constructPow52( S5, S2 );
            Mval = constructPow52( M5, M2 );

            Bval.lshiftMe( shiftBias = Sval.normalizeMe() );
            Mval.lshiftMe( shiftBias );
            tenSval = Sval.mult( 10 );
            
            ndigit = 0;
            q = Bval.quoRemIteration( Sval );
            Mval = Mval.mult( 10 );
            low  = (Bval.cmp( Mval ) < 0);
            high = (Bval.add( Mval ).cmp( tenSval ) > 0 );
            assert q < 10 : q; 
            if ( (q == 0) && ! high ){
                
                decExp--;
            } else {
                digits[ndigit++] = (char)('0' + q);
            }
            
            if ( decExp < -3 || decExp >= 8 ){
                high = low = false;
            }
            while( ! low && ! high ){
                q = Bval.quoRemIteration( Sval );
                Mval = Mval.mult( 10 );
                assert q < 10 : q;  
                low  = (Bval.cmp( Mval ) < 0);
                high = (Bval.add( Mval ).cmp( tenSval ) > 0 );
                digits[ndigit++] = (char)('0' + q);
            }
            if ( high && low ){
                Bval.lshiftMe(1);
                lowDigitDifference = Bval.cmp(tenSval);
            } else
                lowDigitDifference = 0L; 
        }
        this.decExponent = decExp+1;
        this.digits = digits;
        this.nDigits = ndigit;
        
        if ( high ){
            if ( low ){
                if ( lowDigitDifference == 0L ){

                    if ( (digits[nDigits-1]&1) != 0 ) roundup();
                } else if ( lowDigitDifference > 0 ){
                    roundup();
                }
            } else {
                roundup();
            }
        }
    }

    public String
    toString(){
        
        StringBuffer result = new StringBuffer( nDigits+8 );
        if ( isNegative ){ result.append( '-' ); }
        if ( isExceptional ){
            result.append( digits, 0, nDigits );
        } else {
            result.append( "0.");
            result.append( digits, 0, nDigits );
            result.append('e');
            result.append( decExponent );
        }
        return new String(result);
    }

    public String toJavaFormatString() {
        char result[] = (char[])(perThreadBuffer.get());
        int i = getChars(result);
        return new String(result, 0, i);
    }

    public int getChars(char[] result) {
        assert nDigits <= 19 : nDigits; 
        int i = 0;
        if (isNegative) { result[0] = '-'; i = 1; }
        if (isExceptional) {
            System.arraycopy(digits, 0, result, i, nDigits);
            i += nDigits;
        } else {
            if (decExponent > 0 && decExponent < 8) {
                
                int charLength = Math.min(nDigits, decExponent);
                System.arraycopy(digits, 0, result, i, charLength);
                i += charLength;
                if (charLength < decExponent) {
                    charLength = decExponent-charLength;
                    System.arraycopy(zero, 0, result, i, charLength);
                    i += charLength;
                    result[i++] = '.';
                    result[i++] = '0';
                } else {
                    result[i++] = '.';
                    if (charLength < nDigits) {
                        int t = nDigits - charLength;
                        System.arraycopy(digits, charLength, result, i, t);
                        i += t;
                    } else {
                        result[i++] = '0';
                    }
                }
            } else if (decExponent <=0 && decExponent > -3) {
                result[i++] = '0';
                result[i++] = '.';
                if (decExponent != 0) {
                    System.arraycopy(zero, 0, result, i, -decExponent);
                    i -= decExponent;
                }
                System.arraycopy(digits, 0, result, i, nDigits);
                i += nDigits;
            } else {
                result[i++] = digits[0];
                result[i++] = '.';
                if (nDigits > 1) {
                    System.arraycopy(digits, 1, result, i, nDigits-1);
                    i += nDigits-1;
                } else {
                    result[i++] = '0';
                }
                result[i++] = 'E';
                int e;
                if (decExponent <= 0) {
                    result[i++] = '-';
                    e = -decExponent+1;
                } else {
                    e = decExponent-1;
                }
                
                if (e <= 9) {
                    result[i++] = (char)(e+'0');
                } else if (e <= 99) {
                    result[i++] = (char)(e/10 +'0');
                    result[i++] = (char)(e%10 + '0');
                } else {
                    result[i++] = (char)(e/100+'0');
                    e %= 100;
                    result[i++] = (char)(e/10+'0');
                    result[i++] = (char)(e%10 + '0');
                }
            }
        }
        return i;
    }

    @SuppressWarnings("rawtypes")
	private static ThreadLocal perThreadBuffer = new ThreadLocal() {
            protected synchronized Object initialValue() {
                return new char[26];
            }
        };

    private static final int small5pow[] = {
        1,
        5,
        5*5,
        5*5*5,
        5*5*5*5,
        5*5*5*5*5,
        5*5*5*5*5*5,
        5*5*5*5*5*5*5,
        5*5*5*5*5*5*5*5,
        5*5*5*5*5*5*5*5*5,
        5*5*5*5*5*5*5*5*5*5,
        5*5*5*5*5*5*5*5*5*5*5,
        5*5*5*5*5*5*5*5*5*5*5*5,
        5*5*5*5*5*5*5*5*5*5*5*5*5
    };

    private static final long long5pow[] = {
        1L,
        5L,
        5L*5,
        5L*5*5,
        5L*5*5*5,
        5L*5*5*5*5,
        5L*5*5*5*5*5,
        5L*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
        5L*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5*5,
    };

    private static final int n5bits[] = {
        0,
        3,
        5,
        7,
        10,
        12,
        14,
        17,
        19,
        21,
        24,
        26,
        28,
        31,
        33,
        35,
        38,
        40,
        42,
        45,
        47,
        49,
        52,
        54,
        56,
        59,
        61,
    };

    private static final char infinity[] = { 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y' };
    private static final char notANumber[] = { 'N', 'a', 'N' };
    private static final char zero[] = { '0', '0', '0', '0', '0', '0', '0', '0' };
}

class FDBigInt {
    int nWords; 
    int data[]; 

    public FDBigInt( long v ){
        data = new int[2];
        data[0] = (int)v;
        data[1] = (int)(v>>>32);
        nWords = (data[1]==0) ? 1 : 2;
    }

    public FDBigInt( FDBigInt other ){
        data = new int[nWords = other.nWords];
        System.arraycopy( other.data, 0, data, 0, nWords );
    }

    private FDBigInt( int [] d, int n ){
        data = d;
        nWords = n;
    }

    public void
    lshiftMe( int c )throws IllegalArgumentException {
        if ( c <= 0 ){
            if ( c == 0 )
                return; 
            else
                throw new IllegalArgumentException("negative shift count");
        }
        int wordcount = c>>5;
        int bitcount  = c & 0x1f;
        int anticount = 32-bitcount;
        int t[] = data;
        int s[] = data;
        if ( nWords+wordcount+1 > t.length ){
            
            t = new int[ nWords+wordcount+1 ];
        }
        int target = nWords+wordcount;
        int src    = nWords-1;
        if ( bitcount == 0 ){
            
            System.arraycopy( s, 0, t, wordcount, nWords );
            target = wordcount-1;
        } else {
            t[target--] = s[src]>>>anticount;
            while ( src >= 1 ){
                t[target--] = (s[src]<<bitcount) | (s[--src]>>>anticount);
            }
            t[target--] = s[src]<<bitcount;
        }
        while( target >= 0 ){
            t[target--] = 0;
        }
        data = t;
        nWords += wordcount + 1;

        while ( nWords > 1 && data[nWords-1] == 0 )
            nWords--;
    }

    public int
    normalizeMe() throws IllegalArgumentException {
        int src;
        int wordcount = 0;
        int bitcount  = 0;
        int v = 0;
        for ( src= nWords-1 ; src >= 0 && (v=data[src]) == 0 ; src--){
            wordcount += 1;
        }
        if ( src < 0 ){
            
            throw new IllegalArgumentException("zero value");
        }
        
        nWords -= wordcount;
        
        if ( (v & 0xf0000000) != 0 ){

            for( bitcount = 32 ; (v & 0xf0000000) != 0 ; bitcount-- )
                v >>>= 1;
        } else {
            while ( v <= 0x000fffff ){
                
                v <<= 8;
                bitcount += 8;
            }
            while ( v <= 0x07ffffff ){
                v <<= 1;
                bitcount += 1;
            }
        }
        if ( bitcount != 0 )
            lshiftMe( bitcount );
        return bitcount;
    }

    public FDBigInt
    mult( int iv ) {
        long v = iv;
        int r[];
        long p;

        r = new int[ ( v * ((long)data[nWords-1]&0xffffffffL) > 0xfffffffL ) ? nWords+1 : nWords ];
        p = 0L;
        for( int i=0; i < nWords; i++ ) {
            p += v * ((long)data[i]&0xffffffffL);
            r[i] = (int)p;
            p >>>= 32;
        }
        if ( p == 0L){
            return new FDBigInt( r, nWords );
        } else {
            r[nWords] = (int)p;
            return new FDBigInt( r, nWords+1 );
        }
    }

    public FDBigInt
    mult( FDBigInt other ){
        
        int r[] = new int[ nWords + other.nWords ];
        int i;

        for( i = 0; i < this.nWords; i++ ){
            long v = (long)this.data[i] & 0xffffffffL; 
            long p = 0L;
            int j;
            for( j = 0; j < other.nWords; j++ ){
                p += ((long)r[i+j]&0xffffffffL) + v*((long)other.data[j]&0xffffffffL); 
                r[i+j] = (int)p;
                p >>>= 32;
            }
            r[i+j] = (int)p;
        }
        
        for ( i = r.length-1; i> 0; i--)
            if ( r[i] != 0 )
                break;
        return new FDBigInt( r, i+1 );
    }

    public FDBigInt
    add( FDBigInt other ){
        int i;
        int a[], b[];
        int n, m;
        long c = 0L;

        if ( this.nWords >= other.nWords ){
            a = this.data;
            n = this.nWords;
            b = other.data;
            m = other.nWords;
        } else {
            a = other.data;
            n = other.nWords;
            b = this.data;
            m = this.nWords;
        }
        int r[] = new int[ n ];
        for ( i = 0; i < n; i++ ){
            c += (long)a[i] & 0xffffffffL;
            if ( i < m ){
                c += (long)b[i] & 0xffffffffL;
            }
            r[i] = (int) c;
            c >>= 32; 
        }
        if ( c != 0L ){
            
            int s[] = new int[ r.length+1 ];
            System.arraycopy( r, 0, s, 0, r.length );
            s[i++] = (int)c;
            return new FDBigInt( s, i );
        }
        return new FDBigInt( r, i );
    }

    public int
    cmp( FDBigInt other ){
        int i;
        if ( this.nWords > other.nWords ){

            int j = other.nWords-1;
            for ( i = this.nWords-1; i > j ; i-- )
                if ( this.data[i] != 0 ) return 1;
        }else if ( this.nWords < other.nWords ){

            int j = this.nWords-1;
            for ( i = other.nWords-1; i > j ; i-- )
                if ( other.data[i] != 0 ) return -1;
        } else{
            i = this.nWords-1;
        }
        for ( ; i > 0 ; i-- )
            if ( this.data[i] != other.data[i] )
                break;

        int a = this.data[i];
        int b = other.data[i];
        if ( a < 0 ){
            
            if ( b < 0 ){
                return a-b; 
            } else {
                return 1; 
            }
        } else {
            
            if ( b < 0 ) {
                
                return -1;
            } else {
                return a - b;
            }
        }
    }

    public int
    quoRemIteration( FDBigInt S )throws IllegalArgumentException {

        if ( nWords != S.nWords ){
            throw new IllegalArgumentException("disparate values");
        }

        int n = nWords-1;
        long q = ((long)data[n]&0xffffffffL) / (long)S.data[n];
        long diff = 0L;
        for ( int i = 0; i <= n ; i++ ){
            diff += ((long)data[i]&0xffffffffL) -  q*((long)S.data[i]&0xffffffffL);
            data[i] = (int)diff;
            diff >>= 32; 
        }
        if ( diff != 0L ) {

            long sum = 0L;
            while ( sum ==  0L ){
                sum = 0L;
                for ( int i = 0; i <= n; i++ ){
                    sum += ((long)data[i]&0xffffffffL) +  ((long)S.data[i]&0xffffffffL);
                    data[i] = (int) sum;
                    sum >>= 32; 
                }
                
                assert sum == 0 || sum == 1 : sum; 
                q -= 1;
            }
        }

        long p = 0L;
        for ( int i = 0; i <= n; i++ ){
            p += 10*((long)data[i]&0xffffffffL);
            data[i] = (int)p;
            p >>= 32; 
        }
        assert p == 0L : p; 
        return (int)q;
    }

    public String
    toString() {
        StringBuffer r = new StringBuffer(30);
        r.append('[');
        int i = Math.min( nWords-1, data.length-1) ;
        if ( nWords > data.length ){
            r.append( "("+data.length+"<"+nWords+"!)" );
        }
        for( ; i> 0 ; i-- ){
            r.append( Integer.toHexString( data[i] ) );
            r.append(' ');
        }
        r.append( Integer.toHexString( data[0] ) );
        r.append(']');
        return new String( r );
    }
}
