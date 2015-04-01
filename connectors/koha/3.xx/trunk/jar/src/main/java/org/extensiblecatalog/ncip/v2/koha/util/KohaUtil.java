package org.extensiblecatalog.ncip.v2.koha.util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.extensiblecatalog.ncip.v2.service.AgencyId;
import org.extensiblecatalog.ncip.v2.service.BibliographicItemId;
import org.extensiblecatalog.ncip.v2.service.BlockOrTrap;
import org.extensiblecatalog.ncip.v2.service.BlockOrTrapType;
import org.extensiblecatalog.ncip.v2.service.ComponentId;
import org.extensiblecatalog.ncip.v2.service.ComponentIdentifierType;
import org.extensiblecatalog.ncip.v2.service.ElectronicAddress;
import org.extensiblecatalog.ncip.v2.service.ElectronicAddressType;
import org.extensiblecatalog.ncip.v2.service.FromAgencyId;
import org.extensiblecatalog.ncip.v2.service.HoldingsInformation;
import org.extensiblecatalog.ncip.v2.service.InitiationHeader;
import org.extensiblecatalog.ncip.v2.service.Location;
import org.extensiblecatalog.ncip.v2.service.LocationName;
import org.extensiblecatalog.ncip.v2.service.LocationNameInstance;
import org.extensiblecatalog.ncip.v2.service.LookupItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.LookupItemSetInitiationData;
import org.extensiblecatalog.ncip.v2.service.NCIPInitiationData;
import org.extensiblecatalog.ncip.v2.service.PhysicalAddress;
import org.extensiblecatalog.ncip.v2.service.RequestStatusType;
import org.extensiblecatalog.ncip.v2.service.ResponseHeader;
import org.extensiblecatalog.ncip.v2.service.StructuredAddress;
import org.extensiblecatalog.ncip.v2.service.ToAgencyId;
import org.extensiblecatalog.ncip.v2.service.UserAddressInformation;
import org.extensiblecatalog.ncip.v2.service.Version1AgencyElementType;
import org.extensiblecatalog.ncip.v2.service.Version1BibliographicItemIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.Version1BibliographicRecordIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.Version1ElectronicAddressType;
import org.extensiblecatalog.ncip.v2.service.Version1LocationType;
import org.extensiblecatalog.ncip.v2.service.Version1PhysicalAddressType;
import org.extensiblecatalog.ncip.v2.service.Version1RequestStatusType;
import org.extensiblecatalog.ncip.v2.service.Version1UserAddressRoleType;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

public class KohaUtil {

	public static AgencyId createAgencyId(String agencyId) {
		return new AgencyId(Version1AgencyElementType.VERSION_1_AGENCY_ELEMENT_TYPE, agencyId);
	}

	public static ComponentId createComponentIdAsAccessionNumber(String barcodeValue) {
		ComponentId componentId = new ComponentId();
		componentId.setComponentIdentifierType(new ComponentIdentifierType(Version1BibliographicRecordIdentifierCode.VERSION_1_BIBLIOGRAPHIC_RECORD_IDENTIFIER_CODE,
				Version1BibliographicRecordIdentifierCode.ACCESSION_NUMBER.getValue()));
		componentId.setComponentIdentifier(barcodeValue);
		return componentId;
	}

	public static Location parseLocation(JSONObject kohaItem) {
		Location location = new Location();
		LocationNameInstance locationNameInstance = new LocationNameInstance();

		String locationVal = "";
		locationNameInstance.setLocationNameValue(locationVal);
		// TODO: more to come from requirement for level
		locationNameInstance.setLocationNameLevel(new BigDecimal("1"));// temporarily set to 1.

		List<LocationNameInstance> locationNameInstances = new ArrayList<LocationNameInstance>();
		locationNameInstances.add(locationNameInstance);

		LocationName locationName = new LocationName();
		locationName.setLocationNameInstances(locationNameInstances);

		location = new Location();
		location.setLocationName(locationName);
		location.setLocationType(Version1LocationType.PERMANENT_LOCATION);
		return location;
	}

	public static LocationNameInstance createLocationNameInstance(String locationNameValue, BigDecimal locationNameLevel) {
		LocationNameInstance locationNameInstance = new LocationNameInstance();
		locationNameInstance.setLocationNameValue(locationNameValue);
		locationNameInstance.setLocationNameLevel(locationNameLevel);
		return locationNameInstance;
	}

	public static boolean inDaylightTime() {
		return TimeZone.getDefault().inDaylightTime(new Date());
	}

	public static String convertToKohaDate(GregorianCalendar gregorianCalendar) {
		if (gregorianCalendar == null)
			return null;
		// We need: 31/12/2015

		String month = Integer.toString(gregorianCalendar.get(Calendar.MONTH) + 1);
		if (month.length() < 2)
			month = "0" + month;
		String day = Integer.toString(gregorianCalendar.get(Calendar.DAY_OF_MONTH));
		if (day.length() < 2)
			day = "0" + day;
		return day + "/" + month + "/" + Integer.toString(gregorianCalendar.get(Calendar.YEAR));
	}

	public static BlockOrTrap parseBlockOrTrap(String parsedBlock) {
		BlockOrTrap blockOrTrap = new BlockOrTrap();

		// TODO: Revise this
		blockOrTrap.setBlockOrTrapType(new BlockOrTrapType("http://www.niso.org/ncip/v1_0/imp1/schemes/blockortraptype/blockortraptype.scm", parsedBlock));

		return blockOrTrap;
	}

	/**
	 * Tries to convert date string parsed from koha response to GregorianCalendar format.<br />
	 * Throws SAXException if not successful.
	 * 
	 * @param kohaDateParsed
	 * @return gregorianCalendarDate
	 * @throws SAXException
	 */
	public static GregorianCalendar parseGregorianCalendarFromKohaDate(String kohaDateParsed) throws ParseException {
		if (kohaDateParsed != null && !kohaDateParsed.equalsIgnoreCase("0000-00-00")) {
			GregorianCalendar gregorianCalendarDate = new GregorianCalendar(TimeZone.getDefault());

			gregorianCalendarDate.setTime(KohaConstants.KOHA_DATE_FORMATTER.parse(kohaDateParsed));
			if (inDaylightTime())
				gregorianCalendarDate.add(Calendar.HOUR_OF_DAY, 2);

			return gregorianCalendarDate;
		} else
			return null;
	}

	public static GregorianCalendar parseGregorianCalendarFromKohaLongDate(String kohaLongDateParsed) throws ParseException {
		if (kohaLongDateParsed != null && !kohaLongDateParsed.equalsIgnoreCase("0000-00-00 00:00:00")) {
			GregorianCalendar gregorianCalendarDate = new GregorianCalendar(TimeZone.getDefault());

			gregorianCalendarDate.setTime(KohaConstants.KOHA_DATE_LONG_FORMATTER.parse(kohaLongDateParsed));
			if (inDaylightTime())
				gregorianCalendarDate.add(Calendar.HOUR_OF_DAY, 2);

			return gregorianCalendarDate;
		} else
			return null;
	}

	public static ResponseHeader reverseInitiationHeader(NCIPInitiationData initData) {

		InitiationHeader initiationHeader = initData.getInitiationHeader();
		ResponseHeader responseHeader = null;

		if (initiationHeader != null) {
			responseHeader = new ResponseHeader();

			if (initiationHeader.getFromAgencyId() != null && initiationHeader.getToAgencyId() != null) {
				// Reverse From/To AgencyId because of the request was processed (return to initiator)
				ToAgencyId toAgencyId = new ToAgencyId();
				toAgencyId.setAgencyIds(initiationHeader.getFromAgencyId().getAgencyIds());

				FromAgencyId fromAgencyId = new FromAgencyId();
				fromAgencyId.setAgencyIds(initiationHeader.getToAgencyId().getAgencyIds());

				responseHeader.setFromAgencyId(fromAgencyId);
				responseHeader.setToAgencyId(toAgencyId);
			}
			if (initiationHeader.getFromSystemId() != null && initiationHeader.getToSystemId() != null) {
				responseHeader.setFromSystemId(initiationHeader.getFromSystemId());
				responseHeader.setToSystemId(initiationHeader.getToSystemId());
				if (initiationHeader.getFromAgencyAuthentication() != null && !initiationHeader.getFromAgencyAuthentication().isEmpty())
					responseHeader.setFromSystemAuthentication(initiationHeader.getFromAgencyAuthentication());
			}
		}
		return responseHeader;
	}

	/**
	 * Marks expired tokens as expired.<br>
	 * Note that these expired tokens are removed immediately after new token is created.<br>
	 * The purpose of not removing those earlier is to let user know of the token expiration.
	 * 
	 * @param tokens
	 * @param tokenExpirationTimeInSeconds
	 */
	public static void markExpiredTokens(HashMap<String, ItemToken> tokens, int tokenExpirationTimeInSeconds) {
		long theTime = new Date().getTime();

		for (ItemToken token : tokens.values()) {
			if (token.getTimeCreated() + tokenExpirationTimeInSeconds * 1000 < theTime) {
				token.setExpired();
			}
		}
	}

	/**
	 * Removes expired tokens from memory hashmap.
	 * 
	 * @param tokens
	 */
	public static void purgeExpiredTokens(HashMap<String, ItemToken> tokens) {
		for (Map.Entry<String, ItemToken> tokenEntry : tokens.entrySet()) {
			if (tokenEntry.getValue().isExpired())
				tokens.remove(tokenEntry.getKey());
		}
	}

	public static BibliographicItemId createBibliographicItemIdAsISBN(String bibliographicItemIdentifier) {
		BibliographicItemId bibliographicItemId = new BibliographicItemId();
		bibliographicItemId.setBibliographicItemIdentifier(bibliographicItemIdentifier);
		bibliographicItemId.setBibliographicItemIdentifierCode((Version1BibliographicItemIdentifierCode.ISBN));
		return bibliographicItemId;
	}

	public static BibliographicItemId createBibliographicItemIdAsLegalDepositNumber(String bibliographicItemIdentifier) {
		BibliographicItemId bibliographicItemId = new BibliographicItemId();
		bibliographicItemId.setBibliographicItemIdentifier(bibliographicItemIdentifier);
		bibliographicItemId.setBibliographicItemIdentifierCode(Version1BibliographicItemIdentifierCode.LEGAL_DEPOSIT_NUMBER);
		return bibliographicItemId;
	}

	public static BibliographicItemId createBibliographicItemIdAsURI(String bibliographicItemIdentifier) {
		BibliographicItemId bibliographicItemId = new BibliographicItemId();
		bibliographicItemId.setBibliographicItemIdentifier(bibliographicItemIdentifier);
		bibliographicItemId.setBibliographicItemIdentifierCode(Version1BibliographicItemIdentifierCode.URI);
		return bibliographicItemId;
	}

	public static RequestStatusType parseRequestStatusTypeFromZ37StatusNode(String value) {
		RequestStatusType requestStatusType;
		if (value == "S")
			requestStatusType = Version1RequestStatusType.AVAILABLE_FOR_PICKUP;
		else
			requestStatusType = Version1RequestStatusType.IN_PROCESS;
		return requestStatusType;
	}

	public static HoldingsInformation createHoldingsInformationUnscructured(String description) {
		HoldingsInformation holdingsInformation = new HoldingsInformation();
		holdingsInformation.setUnstructuredHoldingsData(description);
		return holdingsInformation;
	}

	/**
	 * Converts LookupItemInitiationData to LookupItemSetInitiationData with respect only to desired services.
	 * 
	 * @param initData
	 * @return luisInitData
	 */
	public static LookupItemSetInitiationData lookupItemInitDataToLUISInitData(LookupItemInitiationData initData) {
		LookupItemSetInitiationData luisInitData = new LookupItemSetInitiationData();

		luisInitData.setBibliographicDescriptionDesired(initData.getBibliographicDescriptionDesired());
		luisInitData.setCirculationStatusDesired(initData.getCirculationStatusDesired());
		luisInitData.setCurrentBorrowerDesired(initData.getCurrentBorrowerDesired());
		luisInitData.setCurrentRequestersDesired(initData.getCurrentRequestersDesired());
		luisInitData.setElectronicResourceDesired(initData.getElectronicResourceDesired());
		luisInitData.setHoldQueueLengthDesired(initData.getHoldQueueLengthDesired());
		luisInitData.setItemDescriptionDesired(initData.getItemDescriptionDesired());
		luisInitData.setItemUseRestrictionTypeDesired(initData.getItemUseRestrictionTypeDesired());
		luisInitData.setLocationDesired(initData.getLocationDesired());
		luisInitData.setPhysicalConditionDesired(initData.getPhysicalConditionDesired());
		luisInitData.setSecurityMarkerDesired(initData.getSecurityMarkerDesired());
		luisInitData.setSensitizationFlagDesired(initData.getSensitizationFlagDesired());

		return luisInitData;
	}

	public static GregorianCalendar parseDateAvailableFromHoldingBranch(String branchCode) {
		Integer hoursToDeliver = LocalConfig.getTransferBranchTime().get(branchCode);

		if (hoursToDeliver != null) {
			GregorianCalendar dateAvailable = new GregorianCalendar();
			if (inDaylightTime())
				dateAvailable.add(Calendar.HOUR_OF_DAY, 2);

			dateAvailable.add(Calendar.HOUR_OF_DAY, hoursToDeliver);
			return dateAvailable;
		} else
			return null;
	}

	public static List<UserAddressInformation> parseUserAddressInformations(JSONObject userInfo) {

		List<UserAddressInformation> userAddressInformations = new ArrayList<UserAddressInformation>();

		String streetnumber = (String) userInfo.get("streetnumber");
		String addressFirstLine = (String) userInfo.get("address");
		String addressSecondLine = (String) userInfo.get("address2");
		String city = (String) userInfo.get("city");
		String state = (String) userInfo.get("state");
		String zipcode = (String) userInfo.get("zipcode");
		String country = (String) userInfo.get("country");

		if (!allOfTheseAreNull(streetnumber, addressFirstLine, addressSecondLine, city)) {

			StructuredAddress structuredAddress = new StructuredAddress();
			structuredAddress.setCountry(country);
			structuredAddress.setLine1(addressFirstLine);
			structuredAddress.setLine2(addressSecondLine);
			structuredAddress.setStreet(streetnumber);
			structuredAddress.setPostOfficeBox(zipcode);
			structuredAddress.setDistrict(city);
			structuredAddress.setRegion(state);

			userAddressInformations.add(createPhysicalAddress(structuredAddress));
		}

		String streetnumber2 = (String) userInfo.get("B_streetnumber");
		String address2FirstLine = (String) userInfo.get("B_address");
		String address2SecondLine = (String) userInfo.get("B_address2");
		String city2 = (String) userInfo.get("B_city");
		String state2 = (String) userInfo.get("B_state");
		String zipcode2 = (String) userInfo.get("B_zipcode");
		String country2 = (String) userInfo.get("B_country");

		if (!allOfTheseAreNull(streetnumber2, address2FirstLine, address2SecondLine, city2)) {

			StructuredAddress structuredAddress = new StructuredAddress();
			structuredAddress.setCountry(country2);
			structuredAddress.setLine1(address2FirstLine);
			structuredAddress.setLine2(address2SecondLine);
			structuredAddress.setStreet(streetnumber2);
			structuredAddress.setPostOfficeBox(zipcode2);
			structuredAddress.setDistrict(city2);
			structuredAddress.setRegion(state2);

			userAddressInformations.add(createPhysicalAddress(structuredAddress));
		}

		String email = (String) userInfo.get("email");
		if (email != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.MAILTO, email));
		}

		String email2 = (String) userInfo.get("emailpro");
		if (email2 != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.MAILTO, email2));
		}

		String email3 = (String) userInfo.get("B_email");
		if (email3 != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.MAILTO, email3));
		}

		String phone = (String) userInfo.get("phone");
		if (phone != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.TEL, phone));
		}

		String phone2 = (String) userInfo.get("phonepro");
		if (phone2 != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.TEL, phone2));
		}

		String phone3 = (String) userInfo.get("B_phone");
		if (phone3 != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.TEL, phone3));
		}

		String mobile = (String) userInfo.get("mobile");
		if (mobile != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.TEL, mobile));
		}

		String fax = (String) userInfo.get("fax");
		if (fax != null) {
			userAddressInformations.add(createElectronicAddress(Version1ElectronicAddressType.FAX, fax));
		}

		if (userAddressInformations.size() != 0)
			return userAddressInformations;
		else
			return null;
	}

	private static UserAddressInformation createPhysicalAddress(StructuredAddress structuredAddress) {
		UserAddressInformation userAddressInformation = new UserAddressInformation();
		userAddressInformation.setUserAddressRoleType(Version1UserAddressRoleType.MULTI_PURPOSE);

		PhysicalAddress physicalAddress = new PhysicalAddress();
		physicalAddress.setPhysicalAddressType(Version1PhysicalAddressType.STREET_ADDRESS);
		physicalAddress.setStructuredAddress(structuredAddress);

		userAddressInformation.setPhysicalAddress(physicalAddress);
		return userAddressInformation;
	}

	private static UserAddressInformation createElectronicAddress(ElectronicAddressType electronicAddressType, String electronicAddressData) {
		UserAddressInformation userAddressInformation = new UserAddressInformation();
		userAddressInformation.setUserAddressRoleType(Version1UserAddressRoleType.MULTI_PURPOSE);

		ElectronicAddress electronicAddress = new ElectronicAddress();
		electronicAddress.setElectronicAddressType(electronicAddressType);
		electronicAddress.setElectronicAddressData(electronicAddressData);

		userAddressInformation.setElectronicAddress(electronicAddress);
		return userAddressInformation;
	}

	private static boolean allOfTheseAreNull(String... values) {
		for (String value : values) {
			if (value != null)
				return false;
		}
		return true;
	}
}
