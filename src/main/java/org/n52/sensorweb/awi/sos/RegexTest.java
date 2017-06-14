package org.n52.sensorweb.awi.sos;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class RegexTest {
    public static void main(String[] args) {
        List<String> tests = Arrays.asList("station:heluwobs",
                                           "station:heluwobs:fb_730201",
                                           "station:heluwobs:fb_730201:aoa_cs_18_21",
                                           "station:heluwobs:fb_730201:co2_ftco2_0410_001",
                                           "station:heluwobs:fb_730201:depth_9999a",
                                           "station:heluwobs:fb_730201:fsi_9999a",
                                           "station:heluwobs:fb_730201:optode_9999a",
                                           "station:heluwobs:fb_730201:sbe38_9999a",
                                           "station:heluwobs:fb_730201:turbidity_9999a",
                                           "station:neumayer_iii",
                                           "station:neumayer_iii:meteorological_observatory",
                                           "station:svluwobs",
                                           "station:svluwobs:svluw2:adcp_17374",
                                           "station:svluwobs:svluw2:adcp_17374:east",
                                           "station:svluwobs:svluw2:adcp_17374:north",
                                           "station:svluwobs:svluw2:adcp_23789",
                                           "station:svluwobs:svluw2:adcp_23789:east",
                                           "station:svluwobs:svluw2:adcp_23789:north",
                                           "station:svluwobs:svluw2:ctd_181",
                                           "station:svluwobs:svluw2:sbe38_657",
                                           "vessel:heincke",
                                           "vessel:heincke:dwd",
                                           "vessel:heincke:trimble",
                                           "vessel:heincke:tsg:sbe38",
                                           "vessel:ms_hel",
                                           "vessel:ms_hel:fb_740602:aoa_9999a",
                                           "vessel:ms_hel:fb_740602:fsi_9999a",
                                           "vessel:ms_hel:fb_740602:optode_9999a",
                                           "vessel:ms_hel:fb_740602:ph_9999a",
                                           "vessel:ms_hel:fb_740602:sbe_9999a",
                                           "vessel:ms_hel:gps_mshel",
                                           "vessel:mya-ii",
                                           "vessel:mya-ii:fb_741202:co2_9999a",
                                           "vessel:mya-ii:fb_741202:cyclops_7_9999a",
                                           "vessel:mya-ii:fb_741202:fsi_9999a",
                                           "vessel:mya-ii:fb_741202:optode_9999a",
                                           "vessel:mya-ii:fb_741202:ph_9999a",
                                           "vessel:mya-ii:fb_741202:sbe38_9999a",
                                           "vessel:polarstern",
                                           "vessel:polarstern:bww",
                                           "vessel:polarstern:fb_ps",
                                           "vessel:polarstern:trimb_1",
                                           "vessel:polarstern:trimb_2",
                                           "vessel:polarstern:tsk");


        Pattern pattern = Pattern.compile("^([^:]+:[^:]+)(?::(.+))?$");

        for (String test : tests) {
            Matcher m = pattern.matcher(test);
            if (m.matches()) {
                System.out.printf("platform: %s, device: %s\n", m.group(1), m.group(2));
            } else {
                System.out.printf("NO MATCH: %s\n", test);
            }
        }

    }
}
