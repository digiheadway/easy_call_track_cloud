class Strings {
  static const String supportPhone = '919253029002';
  static const String whatsappLink =
      "whatsapp://send?text=__text__&phone=%2B__phone__";
  static const String appShareLink =
      'https://play.google.com/store/apps/details?id=com.tiffin.service.management.crm';
  static const String tiffincrmShareText =
      "Manage Your Daily Deliveries from Our Tiffin Service.\n\nLogin to \nhttps://imeals.in/?vid=vendor_id\n\nTutorial to Use tiffincrm\nhttps://youtu.be/Yq5wBU_zyFQ?utm_source=vid_vendor_id";

  static const String trackCustomerTiffinText = "Track Customer Tiffins";

  static const String tableStyling = """
<style> 
  * {
      font-family: "Open Sans", sans-serif;
      font-optical-sizing: auto;
      font-style: normal;
      font-variation-settings: "wdth" 100;
      box-sizing: border-box;
    }
    body, html {
      padding: 20px;
      margin: 0;
      color: #2e2e2e;
    }

    header {
      margin: 30px auto;
      margin-bottom: 10px;
    }
    h1 {
      font-size: 24px;
      padding: 20px;
      color: transparent;
      background-clip: text;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-image: linear-gradient(90deg, #6078fe 0%, #4b13e9 100%);
      margin: 0;
      margin-top: 20px;
      text-align: center;
    } 

    hr {
      border: none;
      height: 1px;
      background: linear-gradient(90deg, #6078fe 0%, #4b13e9 100%);
      width: 30%;
      margin: 0px auto;
    }

    .contact_container {
      margin: 20px;
    }

    table {
      width: 100%;
      margin-bottom: 100px;
    }

    .right_border, td[rowspan] {
        border-right: 1px solid rgb(224, 224, 224);
    }

    table, th, td { 
        padding: 10px;
        border: 1px solid rgb(224, 224, 224);
        border-width: 1px 0 0px 0; 
        text-align: center;
    } 

    th {
      border: none;
    }

    table {
      border: 1px solid rgb(224, 224, 224);
      border-radius: 10px;
      padding: 0px;
      overflow:hidden ;
    }


    th:first-of-type {
      border-top-left-radius: 10px;
    }
    th:last-of-type {
      border-top-right-radius: 10px;
    }
    tr:last-of-type td:first-of-type {
      border-bottom-left-radius: 10px;
    }
    tr:last-of-type td:last-of-type {
      border-bottom-right-radius: 10px;
    }

    .box {
      border: 1px solid #eee;
      padding: 10px;
      background-color: #f5f7ff;
      border-radius: 10px;
      margin: 10px;
      box-shadow: #fafbff 0px 0px 10px 0px;
    }

    p, h2 {
      margin: 2px;
    }

    .flex {
      display: flex;
      flex-direction: row;
      justify-content: space-between;
    }
    .flex-col {
      display: flex;
      flex-direction: column;
    }
    .summary_box {
      margin: 0px auto;
      padding: 10px;
      font-size: 12px;
    }      
        </style> 
""";
}
