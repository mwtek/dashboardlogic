/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 * YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 * COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 * OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 * this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboardlogic.tools;

import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_1;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_2;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_3;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_4;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_5;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_6;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_7;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_8;
import static de.ukbonn.mwtek.dashboardlogic.logic.current.CurrentConsent.BCT_MOD_9;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.BIOMAT_ADDITIONAL_QUANTITIES;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.BIOMAT_RETRO_SCIENTIFIC_USAGE_DSGVO;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.BIOMAT_RETRO_STORAGE_PROCESS;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.BIOMAT_SCIENTIFIC_USAGE_DSGVO;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.BIOMAT_STORE_TRANSFER;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.IDAT_SAVE_PROCESS;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.KKDAT_5_YEARS_PROSPECTIVE_TRANSFER;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.KKDAT_5_YEARS_PROSPECTIVE_TRANSFER_KVNR;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.KKDAT_5_YEARS_RETRO_SCIENTIFIC_USAGE;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.KKDAT_5_YEARS_RETRO_TRANSFER_SAVE_USAGE;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MDAT_RETRO_SAVE_PROCESS;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MDAT_SAVE_PROCESS;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MDAT_SCIENTIFIC_USAGE_DSGVO;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_BIOMAT_ADDITIONAL_SAMPLING;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_BIOMAT_COLLECT_STORE_USE;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_BIOMAT_RETRO_STORING_USAGE;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_KKDAT_PROSPECTIVE_TRANSFER_SAVE_USAGE;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_KKDAT_RETRO_TRANSFER_SAVE_USAGE;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_PATDAT_RETRIEVAL_SAVING_USING;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_PATDAT_RETROSPECTIVE_USAGE;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_RECONTACTING_ADDITIONAL_FINDING;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.MOD_RECONTACTING_ADDITIONS;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.RECONTACTING_ADDITIONAL_FINDING_LVL_2;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.RECONTACTING_FURTHER_COLLECTION;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.RECONTACTING_FURTHER_STUDIES;
import static de.ukbonn.mwtek.utilities.enums.MiiConsentPolicyValueSet.RECONTACTING_MERGING_DBS;

import java.util.List;

/** A BCT module assignment happens via l1 code or a combination of (multiple) l2 codes */
public record BctModuleRule(String levelOneKey, List<List<String>> levelTwoKeys) {

  public static List<BctModuleRule> getBctModuleRules() {
    return List.of(
        new BctModuleRule(
            BCT_MOD_1,
            List.of(
                List.of(MOD_PATDAT_RETRIEVAL_SAVING_USING.getCode()),
                List.of(
                    IDAT_SAVE_PROCESS.getCode(),
                    MDAT_SAVE_PROCESS.getCode(),
                    MDAT_SCIENTIFIC_USAGE_DSGVO.getCode()))),
        new BctModuleRule(
            BCT_MOD_2,
            List.of(
                List.of(MOD_PATDAT_RETROSPECTIVE_USAGE.getCode()),
                List.of(MDAT_RETRO_SAVE_PROCESS.getCode()))),
        new BctModuleRule(
            BCT_MOD_3,
            List.of(
                List.of(MOD_KKDAT_RETRO_TRANSFER_SAVE_USAGE.getCode()),
                List.of(
                    KKDAT_5_YEARS_RETRO_TRANSFER_SAVE_USAGE.getCode(),
                    KKDAT_5_YEARS_RETRO_SCIENTIFIC_USAGE.getCode()))),
        new BctModuleRule(
            BCT_MOD_4,
            List.of(
                List.of(MOD_KKDAT_PROSPECTIVE_TRANSFER_SAVE_USAGE.getCode()),
                List.of(
                    KKDAT_5_YEARS_PROSPECTIVE_TRANSFER.getCode(),
                    KKDAT_5_YEARS_PROSPECTIVE_TRANSFER_KVNR.getCode()))),
        new BctModuleRule(
            BCT_MOD_5,
            List.of(
                List.of(MOD_BIOMAT_COLLECT_STORE_USE.getCode()),
                List.of(BIOMAT_STORE_TRANSFER.getCode(), BIOMAT_SCIENTIFIC_USAGE_DSGVO.getCode()))),
        new BctModuleRule(
            BCT_MOD_6,
            List.of(
                List.of(MOD_BIOMAT_ADDITIONAL_SAMPLING.getCode()),
                List.of(BIOMAT_ADDITIONAL_QUANTITIES.getCode()))),
        new BctModuleRule(
            BCT_MOD_7,
            List.of(
                List.of(MOD_BIOMAT_RETRO_STORING_USAGE.getCode()),
                List.of(
                    BIOMAT_RETRO_STORAGE_PROCESS.getCode(),
                    BIOMAT_RETRO_SCIENTIFIC_USAGE_DSGVO.getCode()))),
        new BctModuleRule(
            BCT_MOD_8,
            List.of(
                List.of(MOD_RECONTACTING_ADDITIONS.getCode()),
                List.of(
                    RECONTACTING_MERGING_DBS.getCode(),
                    RECONTACTING_FURTHER_COLLECTION.getCode(),
                    RECONTACTING_FURTHER_STUDIES.getCode()))),
        new BctModuleRule(
            BCT_MOD_9,
            List.of(
                List.of(MOD_RECONTACTING_ADDITIONAL_FINDING.getCode()),
                List.of(RECONTACTING_ADDITIONAL_FINDING_LVL_2.getCode()))));
  }

  /**
   * Resolves the array index for the given module key.
   *
   * @param moduleKey The module identifier
   * @return Zero-based module index
   */
  public static int getModuleIndex(String moduleKey) {
    return switch (moduleKey) {
      case BCT_MOD_1 -> 0;
      case BCT_MOD_2 -> 1;
      case BCT_MOD_3 -> 2;
      case BCT_MOD_4 -> 3;
      case BCT_MOD_5 -> 4;
      case BCT_MOD_6 -> 5;
      case BCT_MOD_7 -> 6;
      case BCT_MOD_8 -> 7;
      case BCT_MOD_9 -> 8;
      default -> throw new IllegalArgumentException("Unknown module key: " + moduleKey);
    };
  }

  public static final List<String> MODULE_KEYS =
      List.of(
          BCT_MOD_1, BCT_MOD_2, BCT_MOD_3, BCT_MOD_4, BCT_MOD_5, BCT_MOD_6, BCT_MOD_7, BCT_MOD_8,
          BCT_MOD_9);
}
