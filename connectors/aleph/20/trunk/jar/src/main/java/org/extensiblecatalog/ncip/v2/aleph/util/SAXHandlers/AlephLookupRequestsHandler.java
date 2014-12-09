package org.extensiblecatalog.ncip.v2.aleph.util.SAXHandlers;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.extensiblecatalog.ncip.v2.aleph.util.AlephConstants;
import org.extensiblecatalog.ncip.v2.aleph.util.AlephException;
import org.extensiblecatalog.ncip.v2.aleph.util.AlephUtil;
import org.extensiblecatalog.ncip.v2.aleph.util.LocalConfig;
import org.extensiblecatalog.ncip.v2.service.BibliographicDescription;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.MediumType;
import org.extensiblecatalog.ncip.v2.service.PickupLocation;
import org.extensiblecatalog.ncip.v2.service.RequestId;
import org.extensiblecatalog.ncip.v2.service.RequestStatusType;
import org.extensiblecatalog.ncip.v2.service.RequestType;
import org.extensiblecatalog.ncip.v2.service.RequestedItem;
import org.extensiblecatalog.ncip.v2.service.Version1ItemIdentifierType;
import org.extensiblecatalog.ncip.v2.service.Version1RequestStatusType;
import org.extensiblecatalog.ncip.v2.service.Version1RequestType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AlephLookupRequestsHandler extends DefaultHandler {

	private TimeZone localTimeZone = TimeZone.getTimeZone("ECT");

	private BibliographicDescription bibliographicDescription;

	private boolean localizationDesired = false;

	private List<RequestedItem> requestedItems;
	private RequestedItem currentRequestedItem;

	// Required to build unique item id
	private String bibDocNumber;
	private String itemSequence;
	private String itemDocNumber;
	private LocalConfig localConfig;
	private boolean itemFullIdFound;

	// Dates
	private boolean earliestDateNeededReached = false;
	private boolean needBeforeDateReached = false;
	private boolean datePlacedReached = false;
	private boolean pickupExpiryDateReached = false;
	private boolean hourPlacedReached = false;
	private boolean pickupDateReached = false;

	// Request specifics
	private boolean reminderLevelReached = false;
	private boolean requestTypeReached = false;
	private boolean z37statusReached = false;

	// Item identifiers
	private boolean bibDocNoReached = false;
	private boolean itemDocNoReached = false;
	private boolean itemSequenceReached = false;

	// Bibliographic description
	private boolean materialReached = false;
	private boolean statusReached = false;
	private boolean authorReached = false;
	private boolean isbnReached = false;
	private boolean titleReached = false;
	private boolean publisherReached = false;

	// Item optional fields etc.
	private boolean pickupLocationReached = false;
	private boolean holdQueueLengthReached = false;

	private boolean requestNumberReached;

	public AlephLookupRequestsHandler(LocalConfig localConfig) {
		this.localConfig = localConfig;
		bibliographicDescription = new BibliographicDescription();
	}

	public AlephLookupRequestsHandler() throws AlephException {
		throw new AlephException("Cannot initialize requestItemHandler without bibliographic library.");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (qName.equalsIgnoreCase(AlephConstants.HOLD_REQUEST_NODE)) {
			currentRequestedItem = new RequestedItem();
			if (requestedItems == null)
				requestedItems = new ArrayList<RequestedItem>();
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_OPEN_DATE_NODE)) {
			datePlacedReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_OPEN_HOUR_NODE)) {
			hourPlacedReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_REQUEST_DATE_NODE)) {
			earliestDateNeededReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_END_REQUEST_DATE_NODE)) {
			pickupExpiryDateReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_ITEM_SEQUENCE_NODE)) {
			itemSequenceReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.QUEUE_NODE)) {
			holdQueueLengthReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_PICKUP_LOCATION_NODE)) {
			pickupLocationReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_END_HOLD_DATE_NODE)) {
			needBeforeDateReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_RECALL_TYPE_NODE)) {
			reminderLevelReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_PRIORITY_NODE)) {
			requestTypeReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_HOLD_DATE_NODE)) {
			pickupDateReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_STATUS_NODE)) {
			z37statusReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.STATUS_NODE)) {
			statusReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_AUTHOR_NODE)) {
			authorReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_ISBN_NODE)) {
			isbnReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_TITLE_NODE)) {
			titleReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_PUBLISHER_NODE)) {
			publisherReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_DOC_NUMBER_NODE)) {
			itemDocNoReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_DOC_NUMBER_NODE)) {
			bibDocNoReached = true;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_MATERIAL_NODE)) {
			materialReached = true;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		if (qName.equalsIgnoreCase(AlephConstants.HOLD_REQUEST_NODE)) {
			if (itemFullIdFound) {
				String itemIdVal = AlephUtil.buildAlephItemId(localConfig, bibDocNumber, itemDocNumber, itemSequence);

				ItemId itemId = new ItemId();
				itemId.setItemIdentifierValue(itemIdVal);
				itemId.setItemIdentifierType(Version1ItemIdentifierType.ACCESSION_NUMBER);

				currentRequestedItem.setItemId(itemId);
			}
			currentRequestedItem.setBibliographicDescription(bibliographicDescription);
			requestedItems.add(currentRequestedItem);
			bibliographicDescription = new BibliographicDescription();
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_OPEN_DATE_NODE) && datePlacedReached) {
			datePlacedReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_OPEN_HOUR_NODE) && hourPlacedReached) {
			hourPlacedReached = false;
			itemFullIdFound = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_REQUEST_DATE_NODE) && earliestDateNeededReached) {
			earliestDateNeededReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_END_REQUEST_DATE_NODE) && pickupExpiryDateReached) {
			pickupExpiryDateReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_ITEM_SEQUENCE_NODE) && itemSequenceReached) {
			itemSequenceReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.QUEUE_NODE) && holdQueueLengthReached) {
			holdQueueLengthReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_PICKUP_LOCATION_NODE) && pickupLocationReached) {
			pickupLocationReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_END_HOLD_DATE_NODE) && needBeforeDateReached) {
			needBeforeDateReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_RECALL_TYPE_NODE) && reminderLevelReached) {
			reminderLevelReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_PRIORITY_NODE) && requestTypeReached) {
			requestTypeReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_HOLD_DATE_NODE) && pickupDateReached) {
			pickupDateReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z37_STATUS_NODE) && z37statusReached) {
			z37statusReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.STATUS_NODE) && statusReached) {
			statusReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_AUTHOR_NODE) && authorReached) {
			authorReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_ISBN_NODE) && isbnReached) {
			isbnReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_TITLE_NODE) && titleReached) {
			titleReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_PUBLISHER_NODE) && publisherReached) {
			publisherReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_DOC_NUMBER_NODE) && itemDocNoReached) {
			itemFullIdFound = false;
			itemDocNoReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z13_DOC_NUMBER_NODE) && bibDocNoReached) {
			itemFullIdFound = false;
			bibDocNoReached = false;
		} else if (qName.equalsIgnoreCase(AlephConstants.Z30_MATERIAL_NODE) && materialReached) {
			materialReached = false;
		}

	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (datePlacedReached) {
			String datePlacedParsed = new String(ch, start, length);
			GregorianCalendar datePlaced = AlephUtil.parseGregorianCalendarFromAlephDate(datePlacedParsed);

			currentRequestedItem.setDatePlaced(datePlaced);

			datePlacedReached = false;
		} else if (hourPlacedReached) {
			String hourPlacedParsed = new String(ch, start, length);
			if (!hourPlacedParsed.equalsIgnoreCase("00000000")) {
				GregorianCalendar datePlaced = currentRequestedItem.getDatePlaced();
				GregorianCalendar hourPlaced = new GregorianCalendar(localTimeZone);

				try {
					hourPlaced.setTime(AlephConstants.ALEPH_HOUR_FORMATTER.parse(hourPlacedParsed));
				} catch (ParseException e) {
					e.printStackTrace();
				}

				datePlaced.add(Calendar.HOUR_OF_DAY, hourPlaced.get(Calendar.HOUR_OF_DAY) - 1);
				datePlaced.add(Calendar.MINUTE, hourPlaced.get(Calendar.MINUTE));

				currentRequestedItem.setDatePlaced(datePlaced);
			}
			hourPlacedReached = false;
		} else if (earliestDateNeededReached) {
			String earliestDateNeededParsed = new String(ch, start, length);
			GregorianCalendar earliestDateNeeded = AlephUtil.parseGregorianCalendarFromAlephDate(earliestDateNeededParsed);

			currentRequestedItem.setEarliestDateNeeded(earliestDateNeeded);

			earliestDateNeededReached = false;
		} else if (needBeforeDateReached) {
			String needBeforeDateParsed = new String(ch, start, length);
			GregorianCalendar needBeforeDate = AlephUtil.parseGregorianCalendarFromAlephDate(needBeforeDateParsed);

			currentRequestedItem.setNeedBeforeDate(needBeforeDate);

			needBeforeDateReached = false;
		} else if (holdQueueLengthReached) {
			// Parse this: <queue>1 request(s) of 4 items</queue>
			String parsedHoldQueueLength = (new String(ch, start, length)).split(" ")[0];
			currentRequestedItem.setHoldQueueLength(new BigDecimal(parsedHoldQueueLength));
			holdQueueLengthReached = false;
		} else if (pickupLocationReached) {
			PickupLocation pickupLocation = new PickupLocation(new String(ch, start, length));
			currentRequestedItem.setPickupLocation(pickupLocation);
			pickupLocationReached = false;
		} else if (pickupExpiryDateReached) {
			String pickupExpiryDateParsed = new String(ch, start, length);
			GregorianCalendar pickupExpiryDate = AlephUtil.parseGregorianCalendarFromAlephDate(pickupExpiryDateParsed);

			currentRequestedItem.setPickupExpiryDate(pickupExpiryDate);

			pickupExpiryDateReached = false;
		} else if (reminderLevelReached) {
			currentRequestedItem.setReminderLevel(new BigDecimal(new String(ch, start, length)));
			reminderLevelReached = false;
		} else if (requestTypeReached) {
			RequestType requestType = null;
			String parsedValue = new String(ch, start, length);
			if (parsedValue == "30") // TODO: Add remaining request types - better FIXME move to AlephUtil
				requestType = Version1RequestType.LOAN;
			else
				requestType = Version1RequestType.ESTIMATE;
			currentRequestedItem.setRequestType(requestType);
			requestTypeReached = false;
		} else if (pickupDateReached) {
			GregorianCalendar pickupDate = AlephUtil.parseGregorianCalendarFromAlephDate(new String(ch, start, length));
			currentRequestedItem.setPickupDate(pickupDate);
			pickupDateReached = false;
		} else if (z37statusReached) {
			String parsedStatus = new String(ch, start, length);
			RequestStatusType requestStatusType;
			if (parsedStatus == "S") {
				requestStatusType = Version1RequestStatusType.AVAILABLE_FOR_PICKUP;
			} else 
				requestStatusType = Version1RequestStatusType.IN_PROCESS;
			
			currentRequestedItem.setRequestStatusType(requestStatusType);
			z37statusReached = false;
		} else if (statusReached) {
			String parsedStatus = new String(ch, start, length);
			bibliographicDescription.setSponsoringBody(parsedStatus);
			statusReached = false;
		} else if (materialReached) {
			MediumType mediumType = AlephUtil.detectMediumType(new String(ch, start, length), localizationDesired);
			bibliographicDescription.setMediumType(mediumType);
			materialReached = false;
		} else if (authorReached) {
			bibliographicDescription.setAuthor(new String(ch, start, length));
			authorReached = false;
		} else if (isbnReached) {
			bibliographicDescription.setBibliographicLevel(null);
			isbnReached = false;
		} else if (titleReached) {
			bibliographicDescription.setTitle(new String(ch, start, length));
			titleReached = false;
		} else if (publisherReached) {
			bibliographicDescription.setPublisher(new String(ch, start, length));
			publisherReached = false;
		} else if (itemDocNoReached) {
			itemDocNumber = new String(ch, start, length);
			itemDocNoReached = false;
		} else if (bibDocNoReached) {
			String parsedBibId = new String(ch, start, length);
			bibDocNumber = parsedBibId;
			bibDocNoReached = false;
		} else if (itemSequenceReached) {
			itemSequence = new String(ch, start, length);
			itemSequenceReached = false;
		} else if (requestNumberReached) {
			String requestIdVal = new String(ch, start, length);

			RequestId requestId = new RequestId();
			requestId.setRequestIdentifierValue(requestIdVal);
			currentRequestedItem.setRequestId(requestId);

			requestNumberReached = false;
		} 
	}

	public List<RequestedItem> getRequestedItems() {
		return requestedItems;
	}

	public void setLocalizationDesired(boolean localizationDesired) {
		this.localizationDesired = localizationDesired;
	}
}
