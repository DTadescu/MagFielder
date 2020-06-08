package shdv.example.magfielder.data


import android.util.Log
import shdv.example.magfielder.Utils.Operation.arrayMulDouble
import java.lang.Exception
import java.lang.Math.*
import kotlin.math.pow
import shdv.example.magfielder.Utils.Operation.cosArray
import shdv.example.magfielder.Utils.Operation.sinArray


object IgrfUtils {
    private val PI = 3.141592653589793240

    fun ggToGeo(_h:Double, gdcolat: Double):GeoParameters{
        val h = _h/1000
        val eqrad = 6378.137
        val flat = 1/298.257223563
        val plrad = eqrad*(1-flat)
        val ctgd = kotlin.math.cos(toRadians(gdcolat))
        val stgd = kotlin.math.sin(toRadians(gdcolat))
        val a2 = eqrad*eqrad
        val a4 = a2*a2
        val b2 = plrad*plrad
        val b4 = b2*b2
        val c2 = ctgd*ctgd
        val s2 = 1-c2
        val rho = kotlin.math.sqrt(a2 * s2 + b2 * c2)

        val rad   = kotlin.math.sqrt(h * (h + 2 * rho) + (a4 * s2 + b4 * c2) / rho.pow(2))
        val cd = (h+rho)/rad
        val sd = (a2-b2)*ctgd*stgd/(rho*rad)
        val cthc  = ctgd*cd - stgd*sd
        val thc = toDegrees(kotlin.math.acos(cthc))

        return GeoParameters(rad, thc, sd, cd)
    }

    fun synthValues(coeffs: ArrayList<Double>, _radius: Double, theta: Double, _phi: Double, nmax:Int = 13):FieldComponents{
        val radius = _radius/6371.2
        //for(c in coeffs) Log.d("SYNTH","${c}")
        if(theta >= 180.0 || theta <= 0.0) throw ModelException("Invalid value.")
        val nmin = 1
        //val nmax = coeffs.size
        if(nmax < nmin) throw ModelException("Invalid value.")
        if(nmax > sqrt(coeffs.size.toDouble())) throw ModelException("Invalid value.")

        var r_n = radius.pow(-(nmin+2))
        val pnm = legendrePoly(nmax, theta)

        val sinth = pnm[1][1]

        val phi = toRadians(_phi)
        val range = DoubleArray(nmax+1)
        for(i in range.indices){
            range[i] = i.toDouble()
        }
        val cmp = cosArray(arrayMulDouble(range, phi))
        val smp = sinArray(arrayMulDouble(range, phi))

        var bRadius = 0.0
        var bTheta = 0.0
        var bPhi = 0.0

        var num = nmin*nmin-1
        for(n in nmin..nmax){
            bRadius += (n+1)*pnm[n][0]*r_n*coeffs[num]
            bTheta += -pnm[0][n+1]*r_n*coeffs[num]
            num++
            for (m in 1..n){
                bRadius += (n+1)*pnm[n][m]*r_n*(coeffs[num]*cmp[m] + coeffs[num+1]*smp[m])
                bTheta += -pnm[m][n+1]*r_n*(coeffs[num]*cmp[m] + coeffs[num+1]*smp[m])

                var divPnm = 0.0
                try {
                    divPnm = pnm[n][m]/sinth
                }
                catch (e:Exception){}
                bPhi += m*divPnm*r_n*(coeffs[num]*smp[m]-coeffs[num+1]*cmp[m])
                num += 2
            }
            r_n /= radius
        }

        return FieldComponents(bRadius, bTheta, bPhi)
    }

    fun legendrePoly(nmax: Int, theta: Double): Array<DoubleArray>{
        val costh = kotlin.math.cos(toRadians(theta))
        val sinth = kotlin.math.sqrt(1 - costh.pow(2))
        val pnm = Array(nmax+1){DoubleArray(nmax+2)}
        for (n in pnm.indices){
            for (m in pnm[n].indices){
                pnm[n][m] = 0.0
            }
        }
        pnm[0][0] = 1.0
        pnm[1][1] = sinth

        var rootn = DoubleArray(((2* (nmax.toDouble().pow(2)) + 1).toInt()))
        for(i in rootn.indices){
            rootn[i] = kotlin.math.sqrt(i.toDouble())
           // Log.d("ROOTN", "${rootn[i]}")
        }

        for(m in 0 until nmax){
            val pnm_tmp = rootn[m+m+1]*pnm[m][m]
            pnm[m+1][m] = costh*pnm_tmp
            if(m > 0){
                pnm[m+1][m+1] = sinth*pnm_tmp/rootn[m+m+2]
            }
            for (n in (m+2)..nmax){
                val d = n*n - m*m
                val e = n + n - 1
                pnm[n][m] = (e*costh*pnm[n-1][m] - rootn[d-e]*pnm[n-2][m])/rootn[d]
            }
        }
        pnm[0][2] = -pnm[1][1]
        pnm[1][2] = pnm[1][0]
        for(n in 2..nmax){
            pnm[0][n+1] = -kotlin.math.sqrt((n * n + n) / 2.0) *pnm[n][1]
            pnm[1][n+1] = (kotlin.math.sqrt(2.0 * (n * n + n)) *pnm[n][0]
                    - kotlin.math.sqrt(n*n+n-2.0)*pnm[n][2])/2
            for(m in 2 until n){
                pnm[m][n+1] = 0.5*(kotlin.math.sqrt((n + m) * (n - m + 1.0)) * pnm[n][m-1]
                        - kotlin.math.sqrt((n + m + 1.0) * (n - m)) * pnm[n][m+1])
            }
            pnm[n][n+1] = kotlin.math.sqrt(2.0 * n) * pnm[n][n-1]/ 2
        }
        return pnm
    }

    fun xyz2dhif(x: Double, y: Double, z: Double): FieldResult{
        val hsq = x*x + y*y
        val bhor = sqrt(hsq)
        val btot = sqrt(hsq + z*z)
        val dec = toDegrees(atan2(y, x))
        val inc = toDegrees(atan2(z, bhor))
        return FieldResult(dec, bhor, inc, btot, x, y, z)
    }


}